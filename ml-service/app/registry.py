"""Catálogo de versiones de modelo en disco.

Cada entrenamiento deja una carpeta inmutable `<artifacts_dir>/<version>/` con `model.joblib` y
`metadata.json`. Este servicio no decide qué versión está activa: esa decisión (y su auditoría)
pertenece al backend, que pide explícitamente la versión con la que quiere puntuar.
"""
from __future__ import annotations

import hashlib
import json
import re
import threading
from dataclasses import dataclass
from pathlib import Path

import joblib

VERSION_PATTERN = r"^[0-9A-Za-z_-]+$"
_VERSION_REGEX = re.compile(VERSION_PATTERN)

MODEL_FILENAME = "model.joblib"
METADATA_FILENAME = "metadata.json"


@dataclass(frozen=True)
class LoadedModel:
    pipeline: object
    version: str
    metadata: dict


class ModelNotFoundError(LookupError):
    pass


class ModelIntegrityError(RuntimeError):
    pass


def sha256_of_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


class ModelRegistry:
    def __init__(self, artifacts_dir: Path):
        self._artifacts_dir = Path(artifacts_dir)
        self._cache: dict[str, LoadedModel] = {}
        self._lock = threading.Lock()

    def list_versions(self) -> list[str]:
        if not self._artifacts_dir.exists():
            return []
        return sorted(
            entry.name
            for entry in self._artifacts_dir.iterdir()
            if entry.is_dir() and _VERSION_REGEX.match(entry.name) and (entry / METADATA_FILENAME).exists()
        )

    def describe(self, version: str) -> dict:
        """Metadata de la versión + si el artefacto en disco coincide con el hash registrado al entrenar."""
        version_dir = self._version_dir(version)
        metadata = json.loads((version_dir / METADATA_FILENAME).read_text())
        return {**metadata, "artifact_verified": self._artifact_matches_metadata(version_dir, metadata)}

    def load(self, version: str) -> LoadedModel:
        cached = self._cache.get(version)
        if cached is not None:
            return cached
        with self._lock:
            if version not in self._cache:
                self._cache[version] = self._load_verified(version)
            return self._cache[version]

    def _load_verified(self, version: str) -> LoadedModel:
        version_dir = self._version_dir(version)
        metadata = json.loads((version_dir / METADATA_FILENAME).read_text())
        if not self._artifact_matches_metadata(version_dir, metadata):
            raise ModelIntegrityError(
                f"El artefacto de la versión '{version}' no coincide con el hash registrado al entrenarla "
                "(o la versión no tiene hash registrado). No se sirve."
            )
        pipeline = joblib.load(version_dir / MODEL_FILENAME)
        return LoadedModel(pipeline=pipeline, version=version, metadata=metadata)

    def _version_dir(self, version: str) -> Path:
        # El formato restringido impide rutas como "../" al construir el path desde la petición.
        if not _VERSION_REGEX.match(version):
            raise ModelNotFoundError(f"Versión de modelo inválida: '{version}'")
        version_dir = self._artifacts_dir / version
        if not (version_dir / METADATA_FILENAME).exists() or not (version_dir / MODEL_FILENAME).exists():
            raise ModelNotFoundError(f"No existe la versión de modelo '{version}'")
        return version_dir

    @staticmethod
    def _artifact_matches_metadata(version_dir: Path, metadata: dict) -> bool:
        recorded = (metadata.get("artifact") or {}).get("sha256")
        return recorded is not None and recorded == sha256_of_file(version_dir / MODEL_FILENAME)
