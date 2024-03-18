package com.asteriod.duck.opengl.util.audio;

import com.nativeutils.NativeUtils;
import org.jitsi.impl.neomedia.MediaServiceImpl;
import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.WASAPISystem;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.MediaUseCase;
import org.jitsi.service.neomedia.device.MediaDevice;
import org.jitsi.utils.MediaType;

import java.io.IOException;
import java.util.List;

public class WasapiAcquirer {
	public static void main(String[] args) {
		try {
			NativeUtils.loadLibraryFromJar("/resources/libHelloJNI.so");
		} catch (IOException e) {
			// This is probably not the best way to handle exception :-)
			e.printStackTrace();
		}
		LibJitsi.start();
		MediaService mediaService = LibJitsi.getMediaService();
		List<MediaDevice> devices = mediaService.getDevices(MediaType.AUDIO, MediaUseCase.ANY);
		for (int i = 0; i < devices.size(); i++) {
			System.out.println(devices.get(i).toString());

		}
	}
}
