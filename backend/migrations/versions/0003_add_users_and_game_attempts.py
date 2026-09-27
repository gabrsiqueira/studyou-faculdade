"""add users, user_id on file_analysis_* and game_attempts

Revision ID: 0003
Revises: 0002
Create Date: 2026-10-17

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0003"
down_revision: Union[str, None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("email", sa.String(length=255), nullable=False),
        sa.Column("password_hash", sa.String(length=255), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_unique_constraint("uq_users_email", "users", ["email"])
    op.create_index("ix_users_email", "users", ["email"])

    op.add_column(
        "file_analysis_results",
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id"), nullable=True),
    )
    op.create_index("ix_file_analysis_results_user_id", "file_analysis_results", ["user_id"])

    op.add_column(
        "file_analysis_errors",
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id"), nullable=True),
    )
    op.create_index("ix_file_analysis_errors_user_id", "file_analysis_errors", ["user_id"])

    op.create_table(
        "game_attempts",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("game_type", sa.String(length=50), nullable=False),
        sa.Column("file_title", sa.String(length=255), nullable=False),
        sa.Column("score_label", sa.String(length=100), nullable=False),
        sa.Column("xp_earned", sa.Integer(), nullable=False),
        sa.Column("best_score_label", sa.String(length=100), nullable=False),
        sa.Column("played_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_index("ix_game_attempts_user_id", "game_attempts", ["user_id"])


def downgrade() -> None:
    op.drop_table("game_attempts")
    op.drop_index("ix_file_analysis_errors_user_id", table_name="file_analysis_errors")
    op.drop_column("file_analysis_errors", "user_id")
    op.drop_index("ix_file_analysis_results_user_id", table_name="file_analysis_results")
    op.drop_column("file_analysis_results", "user_id")
    op.drop_index("ix_users_email", table_name="users")
    op.drop_constraint("uq_users_email", "users", type_="unique")
    op.drop_table("users")
