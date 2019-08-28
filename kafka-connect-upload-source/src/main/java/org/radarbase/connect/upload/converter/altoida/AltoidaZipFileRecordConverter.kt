/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.AccelerationZipFileConverter
import org.radarbase.connect.upload.converter.CsvProcessor
import org.radarbase.connect.upload.converter.ZipFileRecordConverter
import org.radarbase.connect.upload.exception.ProcessorNotFoundException
import org.slf4j.LoggerFactory

class AltoidaZipFileRecordConverter(override val sourceType: String = "altoida-zip") : ZipFileRecordConverter(sourceType) {

    override fun getCsvProcessor(zipEntryName: String): CsvProcessor {
        val entryName = zipEntryName.trim()
        val processorKey = processors.keys.find {entryName.endsWith(it)} ?: throw ProcessorNotFoundException("Could not find registered processor for zipped entry $entryName")
        logger.debug("Processing $entryName with $processorKey processor")
        return processors[processorKey] ?: throw throw ProcessorNotFoundException("No processor found for key $processorKey")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccelerationZipFileConverter::class.java)
        private val processors = listOf<CsvProcessor>(
                AltoidaAccelerationCsvProcessor(),
                AltoidaActionCsvProcessor(),
                AltoidaAttitudeCsvProcessor(),
                AltoidaDiagnosticsCsvProcessor(),
                AltoidaGravityCsvProcessor(),
                AltoidaMagnetometerCsvProcessor(),
                // add metadata processor
                AltoidaObjectCsvProcessor(),
                AltoidaPathCsvProcessor(),
                AltoidaRotationCsvProcessor(),
                AltoidaTouchscreenCsvProcessor()
        ).map { it.schemaType to it }.toMap()
    }

}

