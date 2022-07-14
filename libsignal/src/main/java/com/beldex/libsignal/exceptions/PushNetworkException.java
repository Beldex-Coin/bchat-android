/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package com.beldex.libsignal.exceptions;

import java.io.IOException;

public class PushNetworkException extends IOException {

  public PushNetworkException(Exception exception) {
    super(exception);
  }

  public PushNetworkException(String s) {
    super(s);
  }

}
