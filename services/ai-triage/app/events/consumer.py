"""
Async Kafka consumer worker listening to incident.triage.requested.v1 events.
Dispatches messages to TriagePipelineService and publishes results or DLQ events.
"""

import asyncio
import json
import logging
from typing import Optional

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from app.config import settings
from app.services.triage_pipeline import TriagePipelineService

logger = logging.getLogger(__name__)


class KafkaConsumerWorker:
    def __init__(self):
        self.consumer: Optional[AIOKafkaConsumer] = None
        self.producer: Optional[AIOKafkaProducer] = None
        self.is_running = False
        self.pipeline = TriagePipelineService(
            dlq_publisher=self._publish_dlq,
            completion_publisher=self._publish_completion,
        )

    async def start(self):
        logger.info(f"Connecting Kafka consumer to {settings.kafka_bootstrap_servers}...")
        try:
            self.consumer = AIOKafkaConsumer(
                settings.kafka_topic_triage_requested,
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.kafka_consumer_group_id,
                auto_offset_reset="earliest",
                enable_auto_commit=False,
            )
            self.producer = AIOKafkaProducer(
                bootstrap_servers=settings.kafka_bootstrap_servers
            )
            await self.consumer.start()
            await self.producer.start()
            self.is_running = True
            logger.info("Kafka consumer worker started successfully.")
            asyncio.create_task(self._consume_loop())
        except Exception as e:
            logger.warning(f"Could not connect to Kafka broker (running offline/standalone mode): {e}")

    async def stop(self):
        self.is_running = False
        if self.consumer:
            await self.consumer.stop()
        if self.producer:
            await self.producer.stop()
        logger.info("Kafka consumer worker stopped.")

    async def _consume_loop(self):
        while self.is_running:
            try:
                msg = await self.consumer.getone()
                raw_payload = msg.value.decode("utf-8")
                logger.info(f"Received message from topic {msg.topic} partition {msg.partition}")
                self.pipeline.process_triage_request(raw_payload)
                await self.consumer.commit()
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in consume loop: {e}", exc_info=True)
                await asyncio.sleep(1)

    def _publish_completion(self, event_dict: dict):
        if self.producer and self.is_running:
            key = event_dict.get("payload", {}).get("incidentId", "").encode("utf-8")
            val = json.dumps(event_dict).encode("utf-8")
            asyncio.create_task(
                self.producer.send_and_wait(settings.kafka_topic_triage_completed, key=key, value=val)
            )

    def _publish_dlq(self, dlq_dict: dict):
        if self.producer and self.is_running:
            key = dlq_dict.get("originalEventId", "").encode("utf-8")
            val = json.dumps(dlq_dict).encode("utf-8")
            asyncio.create_task(
                self.producer.send_and_wait(settings.kafka_topic_triage_dlq, key=key, value=val)
            )
