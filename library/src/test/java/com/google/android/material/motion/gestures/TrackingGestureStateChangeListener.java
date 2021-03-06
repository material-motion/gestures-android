/*
 * Copyright 2016-present The Material Motion Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.motion.gestures;

import com.google.android.material.motion.gestures.GestureRecognizer.GestureStateChangeListener;
import com.google.common.collect.Lists;

import java.util.List;

import static com.google.android.material.motion.gestures.GestureRecognizer.POSSIBLE;

/**
 * A GestureStateChangeListener that tracks the state changes. Useful for tests.
 */
public class TrackingGestureStateChangeListener implements GestureStateChangeListener {
  List<Integer> states = Lists.newArrayList(POSSIBLE);

  @Override
  public void onStateChanged(GestureRecognizer gestureRecognizer) {
    states.add(gestureRecognizer.getState());
  }
}
