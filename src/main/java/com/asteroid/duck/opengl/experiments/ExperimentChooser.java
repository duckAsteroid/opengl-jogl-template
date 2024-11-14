package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides arg and sys property way to select an experiment.
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
		return fromArgs(experiments)
						.or(() -> from(experiments, sysProp("experiment")))
						.or(() -> from(experiments, env("experiment")))
						.or(() -> fromConsole(experiments)).orElse(experiments.get(0));
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
			}
			catch(NumberFormatException e) {
				System.err.println("Invalid number, try again...");
			}
		} while (number < 1 || number > experiments.size());
		return Optional.of(experiments.get(number));
	}
}
