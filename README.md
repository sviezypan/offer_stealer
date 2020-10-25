# README #

Purely functional backend system that showcases working with azure data lake, kafka, external api server, zio, zio stream, zio kafka, zlayer, http4s, mongo reactive client and so on.

To launch application, one needs to provide configuration keys & create an offerstealer kafka topic and launch kafka cluster by docker compose up.

create offerstealer topic
bin/kafka-topics.sh --create --topic offerstealer --replication-factor 1 --partitions 1  --bootstrap-server localhost:9092

check that messages are in kafka
bin/kafka-console-consumer.sh --topic offerstealer --from-beginning --bootstrap-server localhost:9092