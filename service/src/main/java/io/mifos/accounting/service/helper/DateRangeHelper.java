/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.accounting.service.helper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateRangeHelper {

  public static String[] split(final String dateRange){
    final String[] dates;
    if (dateRange == null) {
      final String today = LocalDate.now(Clock.systemUTC()).format(DateTimeFormatter.BASIC_ISO_DATE);
      dates = new String[2];
      dates[0] = today;
      dates[1] = today;
    } else {
      dates = dateRange.split("\\.\\.");
    }

    return dates;
  }

}
