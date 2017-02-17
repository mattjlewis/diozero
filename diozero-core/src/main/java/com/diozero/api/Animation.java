package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.pmw.tinylog.Logger;

import com.diozero.api.easing.EasingFunction;
import com.diozero.api.easing.Linear;
import com.diozero.util.DioZeroScheduler;

/**
 * The Animation class constructs objects that represent a single Animation. An
 * Animation consists of a target and an array of segments. A target is the
 * device or list of devices that are being animated. A segment is a short
 * modular animation sequence (i.e. sit, stand, walk, etc). Segments are
 * synchronous and run first-in, first-out.
 */
public class Animation implements Runnable {
	private static final int DEFAULT_FPS = 60;

	private Collection<OutputDeviceInterface> targets;
	/**
	 * Array of values from 0.0 to 1.0 representing the beginning and end of the
	 * animation respectively (default [0, 1]);
	 */
	private float[] cuePoints;
	private int[] cuePointsMillis;
	/**
	 * A 1 or 2 dimensional array of device positions over time. See more on
	 * keyFrames below. (required)
	 */
	private List<AnimationObject.KeyFrame[]> keyFrames;
	/**
	 * An easing function from ease-component to apply to the playback head on
	 * the timeline. See {@link com.diozero.api.easing.Easing Easing} docs for a
	 * list of available easing functions (default: "linear")
	 */
	private EasingFunction easing;
	/**
	 * When true, segment will loop until animation.next() or animation.stop()
	 * is called (default: false)
	 */
	private boolean loop;
	/**
	 * The cuePoint that the animation will loop back to. If the animation is
	 * playing in reverse, this is the point at which the animation will "loop
	 * back" to 1.0 (default: 0.0)
	 */
	private float loopback;
	/**
	 * Controls the speed of the playback head and scales the calculated
	 * duration of this and all subsequent segments until it is changed by
	 * another segment or a call to the speed() method (default: 1.0)
	 */
	private float speed;
	/**
	 * fps: The maximum frames per second for the segment (default: 60)
	 */
	private int fps;
	private int periodMs;
	/**
	 * function to execute when segment is started (default: none)
	 */
	private Action onStart;
	/**
	 * function to execute when segment is paused (default: none)
	 */
	private Action onPause;
	/**
	 * function to execute when animation is stopped (default: none)
	 */
	private Action onStop;
	/**
	 * function to execute when segment is completed (default: none)
	 */
	private Action onSegmentComplete;
	/**
	 * function to execute when segment loops (default: none)
	 */
	private Action onLoop;

	private Future<?> future;
	private List<List<float[]>> segmentValues;
	private int runSegment;
	private int runStep;

	public Animation(Collection<OutputDeviceInterface> targets, int fps, EasingFunction easing, float speed) {
		this.targets = targets;
		if (fps <= 0) {
			fps = DEFAULT_FPS;
		}

		this.fps = fps;
		periodMs = 1000 / fps;

		// Default to Linear
		if (easing == null) {
			easing = Linear::ease;
		}
		this.easing = easing;

		this.speed = speed;
	}

	public int getFps() {
		return fps;
	}

	public boolean getLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	/**
	 * Play the animation. Animation's are set to play by default and this only
	 * needs to be called if the animation has been paused or a segment's speed
	 * property was set to 0.
	 * 
	 * @return Future instance for the background animation thread
	 */
	public Future<?> play() {
		runSegment = 0;
		runStep = 0;
		future = DioZeroScheduler.getNonDaemonInstance().scheduleAtFixedRate(this, 0, periodMs, TimeUnit.MILLISECONDS);
		if (onStart != null) {
			onStart.action();
		}
		return future;
	}

	/**
	 * Pause an animation while retaining the current progress and segment
	 * queue.
	 */
	public void pause() {
		// TODO
	}

	/**
	 * Immediately stop the animation and flush the segment queue.
	 */
	public void stop() {
		if (future != null) {
			future.cancel(true);
			future = null;

			if (onStop != null) {
				onStop.action();
			}
		}
	}

	/**
	 * Jump to the next segment in the queue. This is called automatically when
	 * a segment completes and in most cases should not be called by the user.
	 */
	public void next() {
		// TODO
	}

	/**
	 * Get the current speed
	 * 
	 * @return current speed factor
	 */
	public float getSpeed() {
		return speed;
	}

	public void enqueue(AnimationObject animationObject) {
		enqueue(animationObject.getDuration(), animationObject.getCuePoints(), animationObject.getKeyFrames());
	}
	
	/**
	 * Add a segment to the animation's queue.
	 * 
	 * @param durationMillis
	 *            Time in milliseconds for the entire animation
	 * @param cuePoints
	 *            List of relative time points at which to change to the next
	 *            segment
	 * @param keyFrames
	 *            List of segment values for target
	 */
	public void enqueue(int durationMillis, float[] cuePoints, List<AnimationObject.KeyFrame[]> keyFrames) {
		// Find our timeline endpoints and refresh rate
		float scaled_duration = durationMillis / Math.abs(speed);

		if (cuePoints.length == 0 || cuePoints.length != keyFrames.size()) {
			throw new IllegalArgumentException("cuePoints length (" + cuePoints.length + ") must equal keyFrames length (" + keyFrames.size() + ")");
		}
		if (keyFrames.size() == 0 || keyFrames.get(0).length != targets.size()) {
			throw new IllegalArgumentException("keyFrames length (" + keyFrames.get(0).length
					+ ") must equal number of targets (" + targets.size() + ")");
		}
		this.cuePoints = cuePoints;
		this.keyFrames = keyFrames;

		cuePointsMillis = new int[cuePoints.length];
		int segment = 0;
		for (float cue_point : cuePoints) {
			cuePointsMillis[segment++] = (int) (cue_point * scaled_duration);
		}

		// List of segments, each segment is a list of steps with a value for
		// each target
		// Java generics make it very difficult to have arrays of Collections
		segmentValues = new ArrayList<>(cuePoints.length - 1);

		// Setup first step from key frame start values
		List<float[]> step_values = new ArrayList<>();
		segmentValues.add(step_values);
		float[] tgt_values = new float[targets.size()];
		step_values.add(tgt_values);
		for (int tgt = 0; tgt < targets.size(); tgt++) {
			tgt_values[tgt] = keyFrames.get(0)[tgt].getValue();
		}

		for (segment = 1; segment < cuePoints.length; segment++) {
			Logger.info("Segment={}", Integer.valueOf(segment));
			long time = 0;
			time += periodMs;
			long seg_duration = cuePointsMillis[segment] - cuePointsMillis[segment - 1];
			while (time < seg_duration) {
				// For each target
				tgt_values = new float[targets.size()];
				step_values.add(tgt_values);
				for (int tgt = 0; tgt < targets.size(); tgt++) {
					EasingFunction kf_ef = keyFrames.get(segment-1)[tgt].getEasingFunction();
					EasingFunction easing_func = kf_ef == null ? easing : kf_ef;
					
					AnimationObject.KeyFrame begin = keyFrames.get(segment - 1)[tgt];
					float change = keyFrames.get(segment)[tgt].getValue() - keyFrames.get(segment - 1)[tgt].getValue();
					tgt_values[tgt] = easing_func.ease(time, begin.getValue(), change, seg_duration);
				}
				time += periodMs;
			}

			// Add the segment finish state
			tgt_values = new float[targets.size()];
			step_values.add(tgt_values);
			for (int tgt = 0; tgt < targets.size(); tgt++) {
				tgt_values[tgt] = keyFrames.get(segment)[tgt].getValue();
			}

			if (segment < cuePoints.length - 1) {
				step_values = new ArrayList<>();
				segmentValues.add(step_values);
				Logger.info("Added to segmentValues, size {}", Integer.valueOf(segmentValues.size()));
			}
		}
	}

	@Override
	public void run() {
		float[] tgt_values = segmentValues.get(runSegment).get(runStep);
		int index = 0;
		for (OutputDeviceInterface target : targets) {
			// TODO Handle delta / value
			target.setValue(tgt_values[index++]);
		}
		runStep++;
		if (runStep == segmentValues.get(runSegment).size()) {
			runStep = 0;
			runSegment++;
			Logger.info("New segment {}, segmentValues.size() {}", Integer.valueOf(runSegment),
					Integer.valueOf(segmentValues.size()));
			if (onSegmentComplete != null) {
				onSegmentComplete.action();
			}
			if (runSegment == segmentValues.size()) {
				runSegment = 0;
				Logger.info("Finished");
				if (loop) {
					if (onLoop != null) {
						onLoop.action();
					}
				} else {
					future.cancel(false);
					future = null;
					if (onStop != null) {
						onStop.action();
					}
				}
			}
		}
	}
}
