from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    artifacts_dir: Path = Path("models")

    model_config = SettingsConfigDict(env_prefix="ML_")


@lru_cache
def get_settings() -> Settings:
    return Settings()
