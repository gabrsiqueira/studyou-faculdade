"""add score_percent and file_id to game_attempts

Revision ID: 0004
Revises: 0003
Create Date: 2026-10-18

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0004"
down_revision: Union[str, None] = "0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "game_attempts",
        sa.Column("score_percent", sa.Integer(), nullable=False, server_default="0"),
    )
    op.add_column("game_attempts", sa.Column("file_id", postgresql.UUID(as_uuid=True), nullable=True))


def downgrade() -> None:
    op.drop_column("game_attempts", "file_id")
    op.drop_column("game_attempts", "score_percent")
