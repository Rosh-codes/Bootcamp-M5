package lv.bootcamp.shelter.task5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Task 5: Nested test classes for CSV parsing
 * <p>
 * Practice:
 * - @Nested to organize by scenario
 * - @DisplayName for readable output
 * - Testing Optional results (isPresent, isEmpty)
 * - Testing file I/O with temp files
 * <p>
 * Instructions:
 * Write tests for AnimalCsvParser. Use @Nested classes to group tests by scenario.
 * For file-based tests, use Files.createTempFile() and Files.writeString() to create test data.
 */
@DisplayName("AnimalCsvParser")
class AnimalCsvParserTest {

    private AnimalCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new AnimalCsvParser();
    }

    // ==================== parseRow tests ====================

    @Nested
    @DisplayName("When parsing valid rows")
    class ValidRows {

        @Test
        @DisplayName("parses a complete row into an Animal")
        void shouldParseCompleteRow() {
            var result = parser.parseRow("Buddy,Dog,3,true,2026-01-15");
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Buddy");
            assertThat(result.get().getSpecies()).isEqualTo("Dog");
            assertThat(result.get().getAge()).isEqualTo(3);
            assertThat(result.get().isVaccinated()).isTrue();
        }

        @Test
        @DisplayName("trims whitespace from fields")
        void shouldTrimWhitespace() {
            assertThat(parser.parseRow("  Buddy , Dog , 3 , true , 2026-01-15 ").get().getName())
                    .isEqualTo("Buddy");
        }

        @Test
        @DisplayName("parses vaccinated=false correctly")
        void shouldParseFalseVaccination() {
            assertThat(parser.parseRow("Buddy,Dog,3,false,2026-01-15").get().isVaccinated()).isFalse();
        }
    }

    @Nested
    @DisplayName("When parsing malformed rows")
    class MalformedRows {

        @Test
        @DisplayName("returns empty for null input")
        void shouldReturnEmptyForNull() {
            assertThat(parser.parseRow(null)).isEmpty();
        }

        @Test
        @DisplayName("returns empty for blank input")
        void shouldReturnEmptyForBlank() {
            assertThat(parser.parseRow("   ")).isEmpty();
        }

        @Test
        @DisplayName("returns empty when row has fewer than 5 fields")
        void shouldReturnEmptyForTooFewFields() {
            assertThat(parser.parseRow("Buddy,Dog,3")).isEmpty();
        }

        @Test
        @DisplayName("returns empty when name is missing")
        void shouldReturnEmptyForMissingName() {
            assertThat(parser.parseRow(",Dog,3,true,2026-01-15")).isEmpty();
        }

        @Test
        @DisplayName("returns empty when age is not a number")
        void shouldReturnEmptyForBadAge() {
            assertThat(parser.parseRow("Buddy,Dog,old,true,2026-01-15")).isEmpty();
        }

        @Test
        @DisplayName("returns empty when age is negative")
        void shouldReturnEmptyForNegativeAge() {
            assertThat(parser.parseRow("Buddy,Dog,-1,true,2026-01-15")).isEmpty();
        }

        @Test
        @DisplayName("returns empty when date is invalid")
        void shouldReturnEmptyForBadDate() {
            assertThat(parser.parseRow("Buddy,Dog,3,true,not-a-date")).isEmpty();
        }
    }

    @Nested
    @DisplayName("When handling edge cases")
    class EdgeCases {

        @Test
        @DisplayName("handles vaccinated field as any non-true string → false")
        void shouldTreatNonTrueAsFalse() {
            assertThat(parser.parseRow("Buddy,Dog,3,maybe,2026-01-15").get().isVaccinated()).isFalse();
        }

        @Test
        @DisplayName("handles age 0 as valid")
        void shouldAcceptAgeZero() {
            var result = parser.parseRow("Buddy,Dog,0,true,2026-01-15");
            assertThat(result).isPresent();
            assertThat(result.get().getAge()).isZero();
        }
    }

    // ==================== parseFile tests ====================

    @Nested
    @DisplayName("When parsing a CSV file")
    class ParseFile {

        @Test
        @DisplayName("parses valid rows and counts skipped rows")
        void shouldParseFileAndCountSkipped() throws IOException {
            Path tempFile = Files.createTempFile("test-intake", ".csv");
            try {
                String content = "name,species,age,vaccinated,intakeDate\nBuddy,Dog,3,true,2026-01-15\n"
                        + "Luna,Cat,2,false,2026-01-10\nMax,Dog,5,true,2026-01-20\ninvalid,row\n";
                Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                var result = parser.parseFile(tempFile);
                assertThat(result.animals()).hasSize(3);
                assertThat(result.skippedRows()).isEqualTo(1);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("returns empty result for file with only a header")
        void shouldReturnEmptyForHeaderOnly() throws IOException {
            Path tempFile = Files.createTempFile("test-intake-header", ".csv");
            try {
                Files.writeString(tempFile, "name,species,age,vaccinated,intakeDate", StandardCharsets.UTF_8);
                var result = parser.parseFile(tempFile);
                assertThat(result.animals()).isEmpty();
                assertThat(result.skippedRows()).isZero();
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("throws IOException for non-existent file")
        void shouldThrowForMissingFile() {
            assertThrows(IOException.class, () -> parser.parseFile(Path.of("does-not-exist.csv")));
        }
    }
}
