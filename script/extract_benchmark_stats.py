#!/usr/bin/env python3
import csv
import os
import pathlib
import xml.etree.ElementTree as ET


def main() -> None:
    out = pathlib.Path("build/benchmark-artifacts")
    out.mkdir(parents=True, exist_ok=True)
    result_dir = pathlib.Path("build/test-results/benchmarkTest")
    xml_files = sorted(result_dir.glob("TEST-*.xml"))

    rows: list[tuple[str, str, str, str]] = []
    metric_lines: list[str] = []
    failures: list[tuple[str, str, str]] = []
    for xml_file in xml_files:
        root = ET.parse(xml_file).getroot()
        suite = root.attrib.get("name", xml_file.stem)
        for case in root.findall("testcase"):
            name = case.attrib.get("name", "")
            time_s = float(case.attrib.get("time", "0") or 0)
            status = "passed"
            failure = case.find("failure")
            error = case.find("error")
            skipped = case.find("skipped")
            if failure is not None:
                status = "failed"
                failures.append((suite, name, failure.attrib.get("message", "")))
            elif error is not None:
                status = "error"
                failures.append((suite, name, error.attrib.get("message", "")))
            elif skipped is not None:
                status = "skipped"
            rows.append((suite, name, f"{time_s:.3f}", status))

        for tag in ("system-out", "system-err"):
            text = root.findtext(tag) or ""
            for line in text.splitlines():
                lower = line.lower()
                if (
                    "benchmark" in lower
                    or "slowdown" in lower
                    or "buildMs=" in line
                    or "stepMs=" in line
                    or "SolveMs=" in line
                    or "overhead" in lower
                ):
                    metric_lines.append(line)

    rows.sort(key=lambda row: float(row[2]), reverse=True)

    with (out / "benchmark-testcases.csv").open("w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["suite", "testcase", "time_seconds", "status"])
        writer.writerows(rows)

    (out / "benchmark-metrics.log").write_text(
        "\n".join(metric_lines) + ("\n" if metric_lines else "")
    )

    summary_text = build_summary(xml_files, rows, metric_lines, failures)
    (out / "benchmark-summary.md").write_text(summary_text)

    step_summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary_path:
        with pathlib.Path(step_summary_path).open("a") as f:
            f.write(summary_text)


def build_summary(
    xml_files: list[pathlib.Path],
    rows: list[tuple[str, str, str, str]],
    metric_lines: list[str],
    failures: list[tuple[str, str, str]],
) -> str:
    total_time = sum(float(row[2]) for row in rows)
    summary: list[str] = ["# Benchmark Test Summary", ""]
    if not xml_files:
        summary.append(
            "No benchmark JUnit XML was produced. This usually means Gradle failed before benchmark tests ran, such as compilation, configuration, or dependency setup failure."
        )
    else:
        summary.append(f"Benchmark XML files: {len(xml_files)}")
        summary.append(f"Benchmark test cases: {len(rows)}")
        summary.append(f"Total reported testcase time: {total_time:.3f}s")
        summary.append(f"Metric lines captured: {len(metric_lines)}")
        if failures:
            summary.append("")
            summary.append("## Failures")
            for suite, name, message in failures:
                summary.append(f"- `{suite}#{name}`: {message}")
        summary.append("")
        summary.append("## Slowest Cases")
        for suite, name, time_s, status in rows[:20]:
            summary.append(f"- {time_s}s `{suite}#{name}` ({status})")
        summary.append("")
        summary.append("## Interpretation")
        summary.append(
            "- If JUnit XML and metric lines are present, a benchmark failure is likely a benchmark assertion/performance regression or numeric stability failure."
        )
        summary.append(
            "- If no JUnit XML is present, inspect `benchmark-gradle.log` first for compile/configuration failures."
        )
    return "\n".join(summary) + "\n"


if __name__ == "__main__":
    main()
