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

import io.mifos.core.lang.DateConverter;
import io.mifos.core.lang.ServiceException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class DateRangeHelper {

  public static DateRange parse(final String dateRange){
    if (dateRange == null) {
      final LocalDate today = LocalDate.now(Clock.systemUTC());
      return new DateRange(today, today);
    } else {
      final String[] dates = dateRange.split("\\.\\.");
      return new DateRange(parseDateTime(dates[0]), parseDateTime(dates[1]));
    }

  }

  private static LocalDate parseDateTime(final String dateString){
    try{
      return DateConverter.dateFromIsoString(dateString);
    }catch(final DateTimeParseException e){
      throw ServiceException.badRequest("Date {0} must use ISO format",
          dateString);
    }
  }

}
