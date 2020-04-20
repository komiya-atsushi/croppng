GRADLE = ./gradlew
BENCH = java -jar build/libs/croppng-*-jmh.jar

BENCH_OPTS = -f $(NUM_FORKS) -tu $(TIME_UNIT) -rf $(RESULT_FORMAT)
NUM_FORKS = 5
TIME_UNIT = 's'
RESULT_FORMAT = 'csv'

test:
	$(GRADLE) test

bench:
	$(GRADLE) --stop
	$(GRADLE) --no-daemon clean jmhJar
	sleep 10
	$(BENCH) $(BENCH_OPTS) -t 1 -rff 'benchmark-result-t1.csv' >benchmark-console-t1.txt
	sleep 60
	$(BENCH) $(BENCH_OPTS) -t 40 -rff 'benchmark-result-t40.csv' >benchmark-console-t40.txt
