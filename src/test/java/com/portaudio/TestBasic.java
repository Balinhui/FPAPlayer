/*
 * Portable Audio I/O Library
 * Java Binding for PortAudio
 *
 * Based on the Open Source API proposed by Ross Bencina
 * Copyright (c) 2008 Ross Bencina
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * The text above constitutes the entire PortAudio license; however, 
 * the PortAudio community also makes the following non-binding requests:
 *
 * Any person wishing to distribute modifications to the Software is
 * requested to send the modifications to the original developer so that
 * they can be incorporated into the canonical version. It is also 
 * requested that these non-binding requests be included along with the 
 * license above.
 */

package com.portaudio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the Java bindings for PortAudio.
 * 
 * @author Phil Burk
 * 
 */
class TestBasic
{

	@Test
	public void testDeviceCount()
	{
		PortAudio.initialize();
		assertTrue( (PortAudio.getVersion() > 0),  "version invalid" );
		System.out.println( "getVersion  = " + PortAudio.getVersion() );
		System.out.println( "getVersionText  = " + PortAudio.getVersionText() );
		System.out.println( "getDeviceCount  = " + PortAudio.getDeviceCount() );
		assertTrue( (PortAudio.getDeviceCount() > 0), "getDeviceCount" );
		PortAudio.terminate();
	}

	@Test
	public void testListDevices()
	{
		PortAudio.initialize();
		int count = PortAudio.getDeviceCount();
		assertTrue( (count > 0), "getDeviceCount" );
		for( int i = 0; i < count; i++ )
		{
			DeviceInfo info = PortAudio.getDeviceInfo( i );
			System.out.println( "------------------ #" + i );
			System.out.println( "  name              = " + info.name );
			System.out.println( "  hostApi           = " + info.hostApi );
			System.out.println( "  maxOutputChannels = "
					+ info.maxOutputChannels );
			System.out.println( "  maxInputChannels  = "
					+ info.maxInputChannels );
			System.out.println( "  defaultSampleRate = "
					+ info.defaultSampleRate );
			System.out.printf( "  defaultLowInputLatency   = %3d msec\n",
					((int) (info.defaultLowInputLatency * 1000)) );
			System.out.printf( "  defaultHighInputLatency  = %3d msec\n",
					((int) (info.defaultHighInputLatency * 1000)) );
			System.out.printf( "  defaultLowOutputLatency  = %3d msec\n",
					((int) (info.defaultLowOutputLatency * 1000)) );
			System.out.printf( "  defaultHighOutputLatency = %3d msec\n",
					((int) (info.defaultHighOutputLatency * 1000)) );

			assertTrue( (info.maxOutputChannels + info.maxInputChannels) > 0,
					"some channels" );
			assertTrue( (info.maxInputChannels < 64),  "not too many channels" );
			assertTrue( (info.maxOutputChannels < 64), "not too many channels" );
		}

		System.out.println( "defaultInput  = "
				+ PortAudio.getDefaultInputDevice() );
		System.out.println( "defaultOutput = "
				+ PortAudio.getDefaultOutputDevice() );

		PortAudio.terminate();
	}

	@Test
	public void testHostApis()
	{
		PortAudio.initialize();
		int validApiCount = 0;
		for( int hostApiType = 0; hostApiType < PortAudio.HOST_API_TYPE_COUNT; hostApiType++ )
		{
			int hostApiIndex = PortAudio
					.hostApiTypeIdToHostApiIndex( hostApiType );
			if( hostApiIndex >= 0 )
			{
				HostApiInfo info = PortAudio.getHostApiInfo( hostApiIndex );
				System.out.println( "Checking Host API: " + info.name );
				for( int apiDeviceIndex = 0; apiDeviceIndex < info.deviceCount; apiDeviceIndex++ )
				{
					int deviceIndex = PortAudio
							.hostApiDeviceIndexToDeviceIndex( hostApiIndex,
									apiDeviceIndex );
					DeviceInfo deviceInfo = PortAudio
							.getDeviceInfo( deviceIndex );
					assertEquals( hostApiIndex, deviceInfo.hostApi,
							"host api must match up" );
				}
				validApiCount++;
			}
		}

		assertEquals( PortAudio.getHostApiCount(), validApiCount,
				"host api counts" );
	}

	@Test
	public void testListHostApis()
	{
		PortAudio.initialize();
		int count = PortAudio.getHostApiCount();
		assertTrue( (count > 0), "getHostApiCount" );
		for( int i = 0; i < count; i++ )
		{
			HostApiInfo info = PortAudio.getHostApiInfo( i );
			System.out.println( "------------------ #" + i );
			System.out.println( "  version             = " + info.version );
			System.out.println( "  name                = " + info.name );
			System.out.println( "  type                = " + info.type );
			System.out.println( "  deviceCount         = " + info.deviceCount );
			System.out.println( "  defaultInputDevice  = "
					+ info.defaultInputDevice );
			System.out.println( "  defaultOutputDevice = "
					+ info.defaultOutputDevice );
			assertTrue( info.deviceCount > 0,  "some devices" );
		}

		System.out.println( "------\ndefaultHostApi = "
				+ PortAudio.getDefaultHostApi() );
		PortAudio.terminate();
	}

	@Test
	public void testCheckFormat()
	{
		PortAudio.initialize();
		StreamParameters streamParameters = new StreamParameters();
		streamParameters.device = PortAudio.getDefaultOutputDevice();
		int result = PortAudio
				.isFormatSupported( null, streamParameters, 44100 );
		System.out.println( "isFormatSupported returns " + result );
		assertEquals( 0, result, "default output format" );
		// Try crazy channelCount
		streamParameters.channelCount = 8765;
		result = PortAudio.isFormatSupported( null, streamParameters, 44100 );
		System.out.println( "crazy isFormatSupported returns " + result );
		assertTrue( (result < 0), "default output format" );
		PortAudio.terminate();
	}

	static class SineOscillator
	{
		double phase = 0.0;
		double phaseIncrement = 0.01;

		SineOscillator(double freq, int sampleRate)
		{
			phaseIncrement = freq * Math.PI * 2.0 / sampleRate;
		}

		double next()
		{
			double value = Math.sin( phase );
			phase += phaseIncrement;
			if( phase > Math.PI )
			{
				phase -= Math.PI * 2.0;
			}
			return value;
		}
	}

	@Test
	public void testStreamError()
	{
		PortAudio.initialize();
		StreamParameters streamParameters = new StreamParameters();
		streamParameters.sampleFormat = PortAudio.FORMAT_FLOAT_32;
		streamParameters.channelCount = 2;
		streamParameters.device = PortAudio.getDefaultOutputDevice();
		int framesPerBuffer = 256;
		int flags = 0;
		BlockingStream stream = PortAudio.openStream( null, streamParameters,
				44100, framesPerBuffer, flags );

		// Try to write data to a stopped stream.
		Throwable caught = null;
		try
		{
			float[] buffer = new float[framesPerBuffer
					* streamParameters.channelCount];
			stream.write( buffer, framesPerBuffer );
		} catch( Throwable e )
		{
			caught = e;
			e.printStackTrace();
		}

		assertTrue( (caught != null), "caught no exception" );
		assertTrue( caught.getMessage().contains( "stopped" ),
				"exception should say stream is stopped" );

		// Try to write null data.
		caught = null;
		try
		{
			stream.write( (float[]) null, framesPerBuffer );
		} catch( Throwable e )
		{
			caught = e;
			e.printStackTrace();
		}
		assertTrue( (caught != null), "caught no exception" );
		assertTrue( caught.getMessage().contains( "null" ),
				"exception should say stream is stopped" );

		// Try to write short data to a float stream.
		stream.start();
		caught = null;
		try
		{
			short[] buffer = new short[framesPerBuffer
					* streamParameters.channelCount];
			stream.write( buffer, framesPerBuffer );
		} catch( Throwable e )
		{
			caught = e;
			e.printStackTrace();
		}

		assertTrue( (caught != null), "caught no exception" );
		assertTrue( caught.getMessage().contains( "Tried to write short" ),
				"exception should say tried to" );

		stream.close();

		PortAudio.terminate();
	}

	public void checkBlockingWriteFloat( int deviceId, double sampleRate )
	{
		StreamParameters streamParameters = new StreamParameters();
		streamParameters.channelCount = 2;
		streamParameters.device = deviceId;
		streamParameters.suggestedLatency = PortAudio
				.getDeviceInfo( streamParameters.device ).defaultLowOutputLatency;
		System.out.println( "suggestedLatency = "
				+ streamParameters.suggestedLatency );

		int framesPerBuffer = 256;
		int flags = 0;
		BlockingStream stream = PortAudio.openStream( null, streamParameters,
				(int) sampleRate, framesPerBuffer, flags );
        assertNotNull( stream, "got default stream" );

        assertTrue( stream.isStopped(), "stream isStopped" );
        assertFalse( stream.isActive(), "stream isActive" );

		int numFrames = 80000;
		double expected = ((double)numFrames) / sampleRate;
		stream.start();
		long startTime = System.currentTimeMillis();
		double startStreamTime = stream.getTime();
        assertFalse( stream.isStopped(), "stream isStopped" );
        assertTrue( stream.isActive(), "stream isActive" );

		writeSineData( stream, framesPerBuffer, numFrames, (int) sampleRate );
		
		StreamInfo streamInfo = stream.getInfo();
		System.out.println( "inputLatency of a stream = "+ streamInfo.inputLatency );
		System.out.println( "outputLatency of a stream = "+streamInfo.outputLatency );
		System.out.println( "sampleRate of a stream = "+ streamInfo.sampleRate );
		
		assertEquals( 0.0, streamInfo.inputLatency, 0.000001, "inputLatency of a stream " );
		assertTrue( (streamInfo.outputLatency > 0), "outputLatency of a stream " );
		assertEquals( sampleRate, streamInfo.sampleRate, 3, "sampleRate of a stream " );

		double endStreamTime = stream.getTime();
		stream.stop();
		long stopTime = System.currentTimeMillis();

		System.out.println( "startStreamTime = " + startStreamTime );
		System.out.println( "endStreamTime = " + endStreamTime );
		double elapsedStreamTime = endStreamTime - startStreamTime;
		System.out.println( "elapsedStreamTime = " + elapsedStreamTime );
		assertTrue( (elapsedStreamTime > 0.0),
				"elapsedStreamTime: " + elapsedStreamTime );
		assertEquals( expected, elapsedStreamTime, 0.10, "elapsedStreamTime: " );

        assertTrue( stream.isStopped(), "stream isStopped" );
        assertFalse( stream.isActive(), "stream isActive" );
		stream.close();

		double elapsed = (stopTime - startTime) / 1000.0;
		assertEquals( expected, elapsed, 0.20 ,"elapsed time to play" );
	}

	@Test
	public void testBlockingWriteFloat()
	{
		PortAudio.initialize();
		checkBlockingWriteFloat( PortAudio.getDefaultOutputDevice(), 44100 );
		PortAudio.terminate();
	}

	@Test
	public void ZtestWriteEachHostAPI()
	{
		PortAudio.initialize();
		for( int hostApiIndex = 0; hostApiIndex < PortAudio.getHostApiCount(); hostApiIndex++ )
		{
			HostApiInfo hostInfo = PortAudio.getHostApiInfo( hostApiIndex );
			System.out.println( "-------------\nWriting using Host API: " + hostInfo.name );
			int deviceId = hostInfo.defaultOutputDevice;
			System.out.println( "   Device ID  =" + deviceId );
			DeviceInfo deviceInfo = PortAudio.getDeviceInfo( deviceId );
			System.out.println( "   sampleRate =" + deviceInfo.defaultSampleRate );
			try {
				checkBlockingWriteFloat(deviceId,
						(int) deviceInfo.defaultSampleRate);
			} catch (RuntimeException e) {
				if (!e.getMessage().contains("Blocking API not supported yet"))
					throw new RuntimeException(e);
			}
			System.out.println( "Finished with " + hostInfo.name );
		}
		PortAudio.terminate();
	}

	private void writeSineData( BlockingStream stream, int framesPerBuffer,
			int numFrames, int sampleRate )
	{
		float[] buffer = new float[framesPerBuffer * 2];
		SineOscillator osc1 = new SineOscillator( 200.0, sampleRate );
		SineOscillator osc2 = new SineOscillator( 300.0, sampleRate );
		int framesLeft = numFrames;
		while( framesLeft > 0 )
		{
			int index = 0;
			int framesToWrite = Math.min( framesLeft, framesPerBuffer );
			for( int j = 0; j < framesToWrite; j++ )
			{
				buffer[index++] = (float) osc1.next();
				buffer[index++] = (float) osc2.next();
			}
			stream.write( buffer, framesToWrite );
			framesLeft -= framesToWrite;
		}
	}

	private void writeSineDataShort( BlockingStream stream,
			int framesPerBuffer, int numFrames )
	{
		short[] buffer = new short[framesPerBuffer * 2];
		SineOscillator osc1 = new SineOscillator( 200.0, 44100 );
		SineOscillator osc2 = new SineOscillator( 300.0, 44100 );
		int framesLeft = numFrames;
		while( framesLeft > 0 )
		{
			int index = 0;
			int framesToWrite = Math.min( framesLeft, framesPerBuffer );
			for( int j = 0; j < framesToWrite; j++ )
			{
				buffer[index++] = (short) (osc1.next() * 32767);
				buffer[index++] = (short) (osc2.next() * 32767);
			}
			stream.write( buffer, framesToWrite );
			framesLeft -= framesToWrite;
		}
	}

	@Test
	public void testBlockingWriteShort()
	{
		PortAudio.initialize();

		StreamParameters streamParameters = new StreamParameters();
		streamParameters.sampleFormat = PortAudio.FORMAT_INT_16;
		streamParameters.channelCount = 2;
		streamParameters.device = PortAudio.getDefaultOutputDevice();
		streamParameters.suggestedLatency = PortAudio
				.getDeviceInfo( streamParameters.device ).defaultLowOutputLatency;
		System.out.println( "suggestedLatency = "
				+ streamParameters.suggestedLatency );

		int framesPerBuffer = 256;
		int flags = 0;
		BlockingStream stream = PortAudio.openStream( null, streamParameters,
				44100, framesPerBuffer, flags );
        assertNotNull( stream, "got default stream" );

		int numFrames = 80000;
		stream.start();
		long startTime = System.currentTimeMillis();
		writeSineDataShort( stream, framesPerBuffer, numFrames );
		stream.stop();
		long stopTime = System.currentTimeMillis();
		stream.close();

		double elapsed = (stopTime - startTime) / 1000.0;
		double expected = numFrames / 44100.0;
		assertEquals( expected, elapsed, 0.20, "elapsed time to play" );
		PortAudio.terminate();
	}

	@Test
	public void testRecordPlayFloat() throws InterruptedException
	{
		checkRecordPlay( PortAudio.FORMAT_FLOAT_32 );
	}

	@Test
	public void testRecordPlayShort() throws InterruptedException
	{
		checkRecordPlay( PortAudio.FORMAT_INT_16 );
	}

	public void checkRecordPlay( int sampleFormat ) throws InterruptedException
	{
		int framesPerBuffer = 256;
		int flags = 0;
		int sampleRate = 44100;
		int numFrames = sampleRate * 3;
		float[] floatBuffer = null;
		short[] shortBuffer = null;

		PortAudio.initialize();
		StreamParameters inParameters = new StreamParameters();
		inParameters.sampleFormat = sampleFormat;
		inParameters.device = PortAudio.getDefaultInputDevice();

		DeviceInfo info = PortAudio.getDeviceInfo( inParameters.device );
		inParameters.channelCount = Math.min( info.maxInputChannels, 2 );
		System.out.println( "channelCount = " + inParameters.channelCount );
		inParameters.suggestedLatency = PortAudio
				.getDeviceInfo( inParameters.device ).defaultLowInputLatency;

		if( sampleFormat == PortAudio.FORMAT_FLOAT_32 )
		{
			floatBuffer = new float[numFrames * inParameters.channelCount];
		}
		else if( sampleFormat == PortAudio.FORMAT_INT_16 )
		{
			shortBuffer = new short[numFrames * inParameters.channelCount];
		}
		// Record a few seconds of audio.
		BlockingStream inStream = PortAudio.openStream( inParameters, null,
				sampleRate, framesPerBuffer, flags );

		System.out.println( "RECORDING - say something like testing 1,2,3..." );
		inStream.start();

		if( sampleFormat == PortAudio.FORMAT_FLOAT_32 )
		{
			inStream.read( floatBuffer, numFrames );
		}
		else if( sampleFormat == PortAudio.FORMAT_INT_16 )
		{
			inStream.read( shortBuffer, numFrames );
		}
		Thread.sleep( 100 );
		int availableToRead = inStream.getReadAvailable();
		System.out.println( "availableToRead =  " + availableToRead );
		assertTrue( availableToRead > 0,  "getReadAvailable ");

		inStream.stop();
		inStream.close();
		System.out.println( "Finished recording. Begin Playback." );

		// Play back what we recorded.
		StreamParameters outParameters = new StreamParameters();
		outParameters.sampleFormat = sampleFormat;
		outParameters.channelCount = inParameters.channelCount;
		outParameters.device = PortAudio.getDefaultOutputDevice();
		outParameters.suggestedLatency = PortAudio
				.getDeviceInfo( outParameters.device ).defaultLowOutputLatency;

		BlockingStream outStream = PortAudio.openStream( null, outParameters,
				sampleRate, framesPerBuffer, flags );
        assertNotNull( outStream, "got default stream" );

        assertFalse( inStream.isActive(), "inStream isActive" );

		outStream.start();
		Thread.sleep( 100 );
		int availableToWrite = outStream.getWriteAvailable();
		System.out.println( "availableToWrite =  " + availableToWrite );
		assertTrue( availableToWrite > 0, "getWriteAvailable " );

		System.out.println( "inStream = " + inStream );
		System.out.println( "outStream = " + outStream );
        assertFalse( inStream.isActive(), "inStream isActive" );
        assertTrue( outStream.isActive(), "outStream isActive" );
		if( sampleFormat == PortAudio.FORMAT_FLOAT_32 )
		{
			outStream.write( floatBuffer, numFrames );
		}
		else if( sampleFormat == PortAudio.FORMAT_INT_16 )
		{
			outStream.write( shortBuffer, numFrames );
		}
		outStream.stop();

		outStream.close();
		PortAudio.terminate();
	}
}
