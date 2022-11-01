/**
 * Copyright (C) 2020 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.fr

import org.orbeon.oxf.fr.importexport.ImportExportSupport
import org.scalatest.funspec.AnyFunSpec


class FormRunnerImportTest extends AnyFunSpec {

  describe("Excel storage format") {

    val Expected = List(
      ("1",         "date",     "1900-01-01"),
      ("44089",     "date",     "2020-09-15"),
      ("44089.625", "datetime", "2020-09-15T15:00:00"),
      ("0",         "time",     "00:00:00"),
      ("0.5",       "time",     "12:00:00"),
      ("0.625",     "time",     "15:00:00"),
    )

    for ((storage, formatTypeString, iso) <- Expected)
      it(s"must convert from `$storage` to `$iso`") {
        assert(iso == FormRunnerImport.convertDateTime(storage, formatTypeString, use1904windowing = false))
      }
  }

  describe("Excel name mangling and prefixing") {

    val NamesUnchanged = List(
      "C_FOO",
      "C.",
      "C_",
      "O_C1_FOO",
      "B1R",
      "B1_FOO",
      "AA",
      "AA1_",
      "AA1FOO",
      "é",
      "é1",
    )

    val NamesWithDisallowedChars = List(
      "A-B" -> "A_B"
    )

    val NamesToPrefix = List(
      "C"          -> "O_C",
      "c"          -> "O_c",
      "R"          -> "O_R",
      "r"          -> "O_r",
      "B1"         -> "O_B1",
      "C1_FOO"     -> "O_C1_FOO",
      "C123456"    -> "O_C123456",
      "AA1"        -> "O_AA1",
      "AAA1"       -> "O_AAA1",
    )

    val Expected =
      NamesUnchanged.map(name => name -> name) :::
        NamesWithDisallowedChars               :::
        NamesToPrefix

    for ((in, out) <- Expected)
      it(s"must handle `$in`") {
        assert(ImportExportSupport.controlNameToNamedRangeName(in)(ImportExportSupport.DefaultExcelNameManglingConfig) == out)
      }
  }
}
