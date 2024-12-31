package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides various ways to select an experiment.
 * Experiments are located via ServiceLoader, so they must be registered in
 * <code>META-INF/services/com.asteriod.duck.opengl.experiments.Experiment</code>
 */
public class ExperimentChooser implements Supplier<Experiment> {

	private static final Logger LOG = LoggerFactory.getLogger(ExperimentChooser.class);

	private final List<Experiment> experiments;

	public ExperimentChooser() {
		ServiceLoader<Experiment> loader = ServiceLoader.load(Experiment.class);
		this.experiments = loader.stream().map(ServiceLoader.Provider::get).toList();
	}

	@Override
	public Experiment get() {
		final Experiment experiment = fromArgs(experiments)
						.or(() -> from(experiments, sysProp("experiment")))
						.or(() -> from(experiments, env("experiment")))
						.or(() -> fromSwingDialog(experiments))
						.or(() -> fromConsole(experiments))
						.or(() -> fromLastExperiment(experiments))
						.or(() -> experiments.stream().min(Comparator.comparingInt(Experiment::getPriority)))
						.orElse(experiments.get(0));
		return experiment;
	}

	private Optional<Experiment> fromSwingDialog(List<Experiment> experiments) {
		if (isInteractive()) {
			String[] experimentTitles = experiments.stream().map(Experiment::getTitle).toArray(String[]::new);
			String[] descriptions = experiments.stream().map(Experiment::getDescription).toArray(String[]::new);
			JPanel message = new JPanel();
			message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
			JComboBox<String> comboBox = new JComboBox<>(experimentTitles);
			message.add(comboBox);
			JLabel description = new JLabel("Please select above");
			comboBox.addActionListener(e -> {
				int index = comboBox.getSelectedIndex();
				description.setText(descriptions[index]);
				description.setToolTipText(descriptions[index]);
				comboBox.setToolTipText(descriptions[index]);
			});
			message.add(description);
			Optional<String> lastExperiment = readLastExperiment();
			if (lastExperiment.isPresent()) {
				// select the default
				Optional<Experiment> first = experiments.stream().filter(exp -> lastExperiment.get().equalsIgnoreCase(exp.getClass().getName())).findFirst();
				if (first.isPresent()) {
					comboBox.setSelectedItem(first.get().getTitle());
					JLabel countdown = new JLabel();
					Timer timer = new Timer(1000, new ActionListener() {
						int secondsRemaining = 6;

						@Override
						public void actionPerformed(ActionEvent e) {
							if (secondsRemaining <= 0) {
								Frame rootFrame = JOptionPane.getRootFrame();
								rootFrame.dispose();
							} else {
								secondsRemaining--;
								countdown.setText(String.format("%d seconds remaining...", secondsRemaining));
							}
						}
					});
					message.add(countdown);
					timer.setRepeats(true);
					timer.setInitialDelay(0);
					timer.start();
				}
			}
			message.setSize(1024, 500);
			int result = JOptionPane.showConfirmDialog(null, message, "Select an Experiment", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION || (lastExperiment.isPresent() && result == JOptionPane.CLOSED_OPTION)) {
				String selectedTitle = (String) comboBox.getSelectedItem();
				Optional<Experiment> selection = experiments.stream().filter(exp -> selectedTitle.equalsIgnoreCase(exp.getTitle())).findAny();
				if (selection.isPresent()) {
					writeLastExperiment(selection.get());
					return selection;
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<Experiment> fromLastExperiment(List<Experiment> experiments) {
		return readLastExperiment().flatMap(name -> experiments.stream().filter(exp -> exp.getClass().getName().equalsIgnoreCase(name)).findFirst());
	}

	private static Optional<String> readLastExperiment() {
		try {
			return Optional.of(Files.readString(Path.of("last.experiment"), StandardCharsets.UTF_8));
		} catch (IOException ex) {
			LOG.warn("Unable to read last run experiment", ex);
		}
		return Optional.empty();
	}

	private static void writeLastExperiment(Experiment e) {
		try {
			Files.writeString(Path.of("last.experiment"), e.getClass().getName(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException ex) {
			LOG.error("Unable to write last run experiment", ex);
		}
	}

	private Optional<Experiment> fromArgs(List<Experiment> experiments) {
		if (Main.ARGS != null && Main.ARGS.length > 0) {
			return experiments.stream().filter(exp -> Main.ARGS[0].equalsIgnoreCase(exp.getTitle())).findAny();
		}
		System.out.println("No arguments given");
		return Optional.empty();
	}

	public static Supplier<String> sysProp(final String propertyName) {
		return () -> System.getProperty(propertyName);
	}

	public static Supplier<String> env(final String propertyName) {
		return () -> System.getenv(propertyName);
	}

	public static Optional<Experiment> from(List<Experiment> experiments, Supplier<String> propertyGetter) {
		Optional<String> experiment = Optional.ofNullable(propertyGetter.get());
		if (experiment.isPresent()) {
			String experimentName = experiment.get();
			try {
				int number = Integer.parseInt(experimentName);
				if (number > 0 && number <= experiments.size()) {
					return Optional.of(experiments.get(number - 1));
				}
			}
			catch(NumberFormatException e) {
				// ignored - just means this is not a number...
			}
			return experiments.stream().filter(exp -> experimentName.equalsIgnoreCase(exp.getTitle())).findAny();
		}
		return Optional.empty();
	}

	public static Optional<Experiment> fromConsole(List<Experiment> experiments) {
		if (isInteractive()) {
			if (System.console() == null) {
				System.out.println("No console, cannot choose experiment");
				return Optional.empty();
			}
			int number = -1;
			do {
				System.out.println("Please choose an experiment:");
				for (int i = 0; i < experiments.size(); i++) {
					Experiment experiment = experiments.get(i);
					System.out.println("[" + i + "] " + experiment.getTitle() + ": " + experiment.getDescription());
				}
				String line = System.console().readLine();
				try {
					number = Integer.parseInt(line);
				} catch (NumberFormatException e) {
					System.err.println("Invalid number, try again...");
				}
			} while (number < 0 || number > experiments.size());
			Experiment selected = experiments.get(number);
			writeLastExperiment(selected);
			return Optional.of(selected);
		}
		return Optional.empty();
	}

	private static boolean isInteractive() {
		return Main.args().anyMatch((arg) -> arg.equals("--interactive=false"));
	}
}
