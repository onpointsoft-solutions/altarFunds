"""
Management command: create_notification_tables
==============================================
Creates / patches notification tables directly via raw SQL.
Safe to run multiple times (IF NOT EXISTS / ADD COLUMN IF NOT EXISTS).

Usage:
    python manage.py create_notification_tables
"""
from django.core.management.base import BaseCommand
from django.db import connection

# ── Full CREATE TABLE statements ─────────────────────────────────────────────
TABLES_SQL = [
    # fcm_tokens
    (
        "fcm_tokens",
        """CREATE TABLE IF NOT EXISTS `fcm_tokens` (
            `id`         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            `user_id`    BIGINT NOT NULL,
            `token`      VARCHAR(255) NOT NULL,
            `device_id`  VARCHAR(255) DEFAULT NULL,
            `platform`   VARCHAR(10) NOT NULL DEFAULT 'android',
            `is_active`  TINYINT(1) NOT NULL DEFAULT 1,
            `last_used`  DATETIME(6) DEFAULT NULL,
            `created_at` DATETIME(6) NOT NULL,
            `updated_at` DATETIME(6) NOT NULL,
            UNIQUE KEY `fcm_tokens_token_uniq` (`token`),
            INDEX `fcm_user_active_idx` (`user_id`, `is_active`),
            INDEX `fcm_device_idx` (`device_id`),
            CONSTRAINT `fcm_tokens_user_fk`
                FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"""
    ),
    # push_notifications — includes ALL columns (priority, dedup_key, etc.)
    (
        "push_notifications",
        """CREATE TABLE IF NOT EXISTS `push_notifications` (
            `id`                BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            `user_id`           BIGINT NOT NULL,
            `title`             VARCHAR(200) NOT NULL,
            `message`           LONGTEXT NOT NULL,
            `notification_type` VARCHAR(50) NOT NULL DEFAULT 'general',
            `priority`          VARCHAR(10) NOT NULL DEFAULT 'normal',
            `data`              JSON NOT NULL,
            `target_url`        VARCHAR(200) DEFAULT NULL,
            `is_read`           TINYINT(1) NOT NULL DEFAULT 0,
            `delivery_status`   VARCHAR(20) NOT NULL DEFAULT 'queued',
            `retry_count`       SMALLINT UNSIGNED NOT NULL DEFAULT 0,
            `max_retries`       SMALLINT UNSIGNED NOT NULL DEFAULT 3,
            `dedup_key`         VARCHAR(255) DEFAULT NULL,
            `scheduled_for`     DATETIME(6) DEFAULT NULL,
            `created_at`        DATETIME(6) NOT NULL,
            `sent_at`           DATETIME(6) DEFAULT NULL,
            `expires_at`        DATETIME(6) DEFAULT NULL,
            INDEX `pn_user_read_idx` (`user_id`, `is_read`),
            INDEX `pn_status_idx` (`delivery_status`),
            INDEX `pn_dedup_idx` (`dedup_key`),
            INDEX `pn_created_idx` (`created_at`),
            CONSTRAINT `push_notifications_user_fk`
                FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"""
    ),
    # notification_preferences
    (
        "notification_preferences",
        """CREATE TABLE IF NOT EXISTS `notification_preferences` (
            `id`                          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            `user_id`                     BIGINT NOT NULL UNIQUE,
            `push_enabled`                TINYINT(1) NOT NULL DEFAULT 1,
            `email_enabled`               TINYINT(1) NOT NULL DEFAULT 1,
            `devotional_notifications`    TINYINT(1) NOT NULL DEFAULT 1,
            `announcement_notifications`  TINYINT(1) NOT NULL DEFAULT 1,
            `giving_notifications`        TINYINT(1) NOT NULL DEFAULT 1,
            `event_notifications`         TINYINT(1) NOT NULL DEFAULT 1,
            `updated_at`                  DATETIME(6) NOT NULL,
            CONSTRAINT `notif_pref_user_fk`
                FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"""
    ),
    # devotional_shares
    (
        "devotional_shares",
        """CREATE TABLE IF NOT EXISTS `devotional_shares` (
            `id`            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            `user_id`       BIGINT NOT NULL,
            `devotional_id` BIGINT NOT NULL,
            `shared_by_id`  BIGINT NOT NULL,
            `shared_at`     DATETIME(6) NOT NULL,
            `message`       LONGTEXT DEFAULT NULL,
            `is_read`       TINYINT(1) NOT NULL DEFAULT 0,
            INDEX `dev_share_user_read_idx` (`user_id`, `is_read`),
            CONSTRAINT `dev_share_user_fk`
                FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
            CONSTRAINT `dev_share_devotional_fk`
                FOREIGN KEY (`devotional_id`) REFERENCES `devotionals` (`id`) ON DELETE CASCADE,
            CONSTRAINT `dev_share_shared_by_fk`
                FOREIGN KEY (`shared_by_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"""
    ),
]

# ── ALTER TABLE — add columns that may be missing from old table versions ─────
# MySQL 8.0+ supports ADD COLUMN IF NOT EXISTS; older 5.7 does not.
# We catch errors silently for columns that already exist.
ALTER_SQL = [
    ("push_notifications", "priority",        "VARCHAR(10) NOT NULL DEFAULT 'normal'"),
    ("push_notifications", "target_url",      "VARCHAR(200) DEFAULT NULL"),
    ("push_notifications", "delivery_status", "VARCHAR(20) NOT NULL DEFAULT 'queued'"),
    ("push_notifications", "retry_count",     "SMALLINT UNSIGNED NOT NULL DEFAULT 0"),
    ("push_notifications", "max_retries",     "SMALLINT UNSIGNED NOT NULL DEFAULT 3"),
    ("push_notifications", "dedup_key",       "VARCHAR(255) DEFAULT NULL"),
    ("push_notifications", "scheduled_for",   "DATETIME(6) DEFAULT NULL"),
    ("push_notifications", "sent_at",         "DATETIME(6) DEFAULT NULL"),
    ("push_notifications", "expires_at",      "DATETIME(6) DEFAULT NULL"),
]

FAKE_MIGRATION_SQL = """
    INSERT IGNORE INTO `django_migrations` (`app`, `name`, `applied`)
    VALUES ('notifications', '0001_initial', NOW())
"""


class Command(BaseCommand):
    help = 'Creates/patches notification tables via raw SQL (idempotent).'

    def handle(self, *args, **options):
        with connection.cursor() as cursor:

            # 1. Create tables
            self.stdout.write('\n── Creating tables ──────────────────────────────')
            for table_name, sql in TABLES_SQL:
                try:
                    cursor.execute(sql)
                    self.stdout.write(self.style.SUCCESS(f'  ✅  {table_name}'))
                except Exception as e:
                    self.stdout.write(self.style.ERROR(f'  ❌  {table_name}: {e}'))

            # 2. Add missing columns to existing tables
            self.stdout.write('\n── Patching missing columns ─────────────────────')
            for table, col, definition in ALTER_SQL:
                try:
                    cursor.execute(
                        f"ALTER TABLE `{table}` ADD COLUMN `{col}` {definition}"
                    )
                    self.stdout.write(self.style.SUCCESS(f'  ✅  {table}.{col} added'))
                except Exception as e:
                    err = str(e)
                    if 'Duplicate column' in err or '1060' in err:
                        self.stdout.write(f'  ○   {table}.{col} already exists')
                    else:
                        self.stdout.write(self.style.ERROR(f'  ❌  {table}.{col}: {e}'))

            # 3. Ensure migration record exists
            self.stdout.write('\n── Migration record ─────────────────────────────')
            try:
                cursor.execute(FAKE_MIGRATION_SQL)
                self.stdout.write(self.style.SUCCESS('  ✅  django_migrations record ensured'))
            except Exception as e:
                self.stdout.write(self.style.WARNING(f'  ⚠️  {e}'))

        self.stdout.write(self.style.SUCCESS(
            '\nDone. Reload the web app now.\n'
        ))
