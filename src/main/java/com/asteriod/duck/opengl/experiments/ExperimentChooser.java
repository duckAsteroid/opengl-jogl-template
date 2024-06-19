package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Supplier;

public class ExperimentChooser implements Supplier<Experiment> {

	private static final Logger LOG = LoggerFactory.getLogger(ExperimentChooser.class);

	private final List<Experiment> experiments;

	public ExperimentChooser() {
		ServiceLoader<Experiment> loader = ServiceLoader.load(Experiment.class);
		this.experiments = loader.stream().map(ServiceLoader.Provider::get).toList();
	}

	@Override
	public Experiment get() {
		return fromArgs(experiments).or(() -> fromSystemProp(experiments)).or(() -> fromConsole(experiments)).orElse(experiments.get(0));
	}

	private Optional<Experiment> fromArgs(List<Experiment> experiments) {
		if (Main.ARGS != null && Main.ARGS.length > 0) {
			return experiments.stream().filter(exp -> Main.ARGS[0].equalsIgnoreCase(exp.getTitle())).findAny();
		}
		System.out.println("No arguments given");
		return Optional.empty();
	}

	public static Optional<Experiment> fromSystemProp(List<Experiment> experiments) {
		Optional<String> experiment = Optional.ofNullable(System.getProperty("experiment"));
		if (experiment.isPresent()) {
			String experimentName = experiment.get();
			try {
				int number = Integer.parseInt(experimentName);
				if (number > 0 && number <= experiments.size()) {
					return Optional.of(experiments.get(number - 1));
				}
			}
			catch(NumberFormatException e) {
				// ignored
			}
			return experiments.stream().filter(exp -> experimentName.equalsIgnoreCase(exp.getTitle())).findAny();
		}
		else {
			System.out.println("No 'experiment' system property");
		}
		return Optional.empty();
	}

	public static Optional<Experiment> fromConsole(List<Experiment> experiments) {
		if (System.console() == null) {
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
