package com.asteriod.duck.opengl.util.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;

public class OpenALAcquirer {
	public static void main(String[] args) {
		String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
		long device = alcOpenDevice(defaultDeviceName);
		int[] attributes = {0};
		long context = alcCreateContext(device, attributes);
		alcMakeContextCurrent(context);
		ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
		ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

		String devices = alcGetString(0, ALC_CAPTURE_DEVICE_SPECIFIER);
		System.out.println(devices);

		alcDestroyContext(context);
		alcCloseDevice(device);
	}
}
