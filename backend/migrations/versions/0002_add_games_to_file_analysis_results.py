"""add games column to file_analysis_results

Revision ID: 0002
Revises: 0001
Create Date: 2026-10-16

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "file_analysis_results",
        sa.Column("games", postgresql.JSONB(astext_type=sa.Text()), nullable=False, server_default="{}"),
    )
    op.alter_column("file_analysis_results", "games", server_default=None)


def downgrade() -> None:
    op.drop_column("file_analysis_results", "games")
