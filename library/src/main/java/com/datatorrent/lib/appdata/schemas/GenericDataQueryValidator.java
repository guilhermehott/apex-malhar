/*
 *  Copyright (c) 2012-2015 Malhar, Inc.
 *  All Rights Reserved.
 */

package com.datatorrent.lib.appdata.schemas;

import com.datatorrent.lib.appdata.qr.CustomDataValidator;
import com.datatorrent.lib.appdata.qr.Data;

/**
 *
 * @author Timothy Farkas: tim@datatorrent.com
 */
public class GenericDataQueryValidator implements CustomDataValidator
{
  @Override
  public boolean validate(Data query, Object context)
  {
    return true;
  }
}