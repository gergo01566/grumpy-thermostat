# grumpy-thermostat

A REST API for querying aggregated sensor readings (temperature and humidity) from IoT devices, backed by a static CSV file.

---

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- Gradle 9

---

## Running the Application

**Start the server:**
```bash
./gradlew bootRun
```
The server starts on `http://localhost:8080`.

**Run tests:**
```bash
./gradlew test
```

**Data file:**

The application loads `src/main/resources/readings.csv` at startup. To use a different file, override the property:
```bash
./gradlew bootRun --args='--app.data.source=file:/path/to/your/readings.csv'
```
If the file does not exist the application will fail to start — the path is validated at startup. You can also set it in `application.yaml`:
```yaml
app:
  data:
    source: classpath:readings.csv   # or file:/absolute/path/readings.csv
```

---

## API

### `GET /api/v1/readings/stats`

Returns aggregated sensor statistics for one or more devices.

#### Query Parameters

| Parameter   | Type                        | Required | Default          | Description                                              |
|-------------|-----------------------------|----------|------------------|----------------------------------------------------------|
| `deviceIds` | comma-separated strings     | No       | all devices      | One or more device IDs, e.g. `deviceIds=1,2`             |
| `from`      | ISO-8601 timestamp          | No       | —                | Range start (inclusive), e.g. `2026-07-10T00:00:00Z`    |
| `to`        | ISO-8601 timestamp          | No       | —                | Range end (exclusive), e.g. `2026-07-12T00:00:00Z`      |
| `metrics`   | `temperature`, `humidity`   | No       | both             | One or both metrics to include                           |
| `statistic` | `avg`, `min`, `max`         | No       | `avg`            | Aggregation function to apply                            |

Timestamps may include a UTC offset (e.g. `2026-07-10T08:00:00+02:00`); the `+` must be URL-encoded as `%2B`. All timestamps are normalised to UTC internally and returned as UTC in the response. UTC (`Z`) is recommended.

#### Date Range Rules

- `from` and `to` must always be provided **together**. Providing only one returns `400`.
- The interval is **half-open**: `[from, to)` — the `from` timestamp is included, `to` is excluded.
- If **neither** `from` nor `to` is provided, the API operates in **latest mode**: it returns the most recent non-null value for each metric per device, with no aggregation applied.

#### Response

In `LATEST` mode, `from`, `to`, and `statistic` are omitted from the response.
Metric values are `null` when no data is available for that metric in the queried range.
Devices are returned sorted by ID lexicographically (string order: `"1"`, `"10"`, `"2"`).

---

## Example Requests

**Latest readings, all devices:**
```
GET /api/v1/readings/stats
```
```json
{"mode":"LATEST","units":{"humidity":"%","temperature":"C"},"devices":[{"deviceId":"1","metrics":{"temperature":22.0,"humidity":44.0}},{"deviceId":"2","metrics":{"temperature":26.67,"humidity":null}}]}
```

**Average temperature and humidity, device 1, full sample range:**
```
GET /api/v1/readings/stats?deviceIds=1&from=2026-07-10T00:00:00Z&to=2026-07-12T00:00:00Z
```
```json
{"mode":"RANGE","from":"2026-07-10T00:00:00Z","to":"2026-07-12T00:00:00Z","statistic":"AVG","units":{"humidity":"%","temperature":"C"},"devices":[{"deviceId":"1","metrics":{"temperature":24.25,"humidity":42.75}}]}
```

**Maximum temperature, device 1:**
```
GET /api/v1/readings/stats?deviceIds=1&metrics=temperature&statistic=max&from=2026-07-10T00:00:00Z&to=2026-07-12T00:00:00Z
```
```json
{"mode":"RANGE","from":"2026-07-10T00:00:00Z","to":"2026-07-12T00:00:00Z","statistic":"MAX","units":{"humidity":"%","temperature":"C"},"devices":[{"deviceId":"1","metrics":{"temperature":32.0}}]}
```

---

## Error Handling

| Status | Cause                                                      |
|--------|------------------------------------------------------------|
| 400    | Invalid parameter value, unknown metric/statistic, missing or inconsistent date range |
| 404    | One or more requested device IDs do not exist              |
| 405    | HTTP method not allowed (e.g. POST on this endpoint)       |
| 500    | Unexpected server error                                    |

**Example 400:**
```json
{"detail":"Unknown metric 'pressure'. Allowed values: temperature, humidity","instance":"/api/v1/readings/stats","status":400,"title":"Bad Request"}
```

**Example 404:**
```json
{"detail":"Unknown device(s): 99","instance":"/api/v1/readings/stats","status":404,"title":"Not Found","unknownIds":["99"]}
```

---

## Tests

35 tests across four suites, all Spring-free except the controller tests:

| Suite | What it covers |
|---|---|
| `CsvLoaderTest` | Parsing, F→C conversion, null humidity, malformed/skipped rows |
| `ThermostatReadingRepositoryTest` | Sort order, `[from, to)` boundary, unknown device, multi-device isolation |
| `ReadingServiceTest` | avg/min/max, latest mode, defaults, error cases |
| `ReadingControllerAdviceTest` | Full Spring context + real sample CSV; verifies 200 structure, 400/404/405 mappings |

```bash
./gradlew test
open build/reports/tests/test/index.html
```

---

## Architecture

```
CsvLoader → ThermostatReadingRepository (in-memory TreeMap) → ReadingService → ReadingController
```

- **CsvLoader** — parses and validates the CSV file at startup, normalises temperatures to Celsius.
- **ThermostatReadingRepository** — holds readings in a `TreeMap<deviceId, List>` sorted by device ID; each list is sorted by `measureTime`.
- **ReadingService** — resolves defaults, validates the query, aggregates results.
- **ReadingController** — exposes the REST endpoint, parses and converts request parameters.
- **GlobalExceptionHandler** — maps known exceptions to Problem Detail responses; extends `ResponseEntityExceptionHandler` so Spring MVC errors (404, 405) are handled correctly instead of falling through to 500.

---

## Assumptions and Design Decisions

- **Temperature normalisation** — all temperatures are converted to Celsius at load time (`(F - 32) × 5/9`). The original unit is not stored. Noted in the response via `"units": { "temperature": "C" }`.
- **Null metrics** — a `null` value in the response means no data was available for that metric in the queried range (e.g. device 2 never reports humidity). This is intentional and distinct from a missing key.
- **Malformed CSV rows** — rows with invalid dates, unknown temperature units, non-finite numbers (`NaN`, `Infinity`), humidity outside `[0, 100]`, missing device ID, or no metrics at all are skipped with a warning log. One bad row does not abort the load.
- **CSV column order** — the header row is skipped (not parsed). The parser assumes fixed column order: `DeviceId, MeasureTime, Temperature, TempUnit, Humidity`. This assumption is documented here rather than validated at runtime.
- **Unknown device IDs** — requesting a device that does not exist in the data returns `404` with the list of unknown IDs. A mix of known and unknown IDs also returns `404`.
- **Half-open interval** — `[from, to)`: `from` is inclusive, `to` is exclusive. This is standard for time-series ranges and avoids double-counting at boundaries.
- **Rounding** — aggregated values are rounded to 2 decimal places using `HALF_UP` at response time, not during computation. For example, `26.666...°C` (80°F) is returned as `26.67`.
- **"Latest" definition** — for each metric, the most recent reading where the value is non-null. Devices may report at different frequencies, so latest temperature and latest humidity may come from different timestamps. There is no "max age" filter — all historical data is considered.
- **Device ID type** — device IDs are treated as strings. The sample data uses integers, but the API accepts any non-blank string. Devices are returned sorted lexicographically (`"1"`, `"10"`, `"2"`), not numerically.
- **Timestamps** — ISO-8601 timestamps with UTC offset are accepted; UTC (`Z`) is recommended. Offsets are normalised to UTC internally.

---

## Missing / Next Steps

- **`measuredAt` in response** — the response does not include the actual timestamp of the latest reading in `LATEST` mode. A consumer cannot tell whether the data is from today or a year ago.
- **Max age filter** — there is no way to say "only consider readings from the last 24 hours" in latest mode. A `maxAge` parameter (e.g. `maxAge=PT24H`) would address this.
- Pagination for responses with many devices or a wide time range.
- Support for returning temperatures in Fahrenheit via an optional `unit=F` query parameter (conversion is linear, so min/max/avg computed in Celsius can be converted at response time).
- Hot-reload of the CSV file without restarting the server.
- Header-based column detection in the CSV parser (currently position-based).
- Authentication and authorisation.

---

## Scaling to Millions of Rows

The current in-memory model loads the entire CSV into a `TreeMap` at startup. For millions of rows this would be impractical.

**In memory (without a database):** readings per device are already sorted by `measureTime`, so `findInRange` could use binary search instead of the current linear scan. For min/max/avg over large ranges, prefix aggregates or sparse tables would avoid scanning the full list on every request.
