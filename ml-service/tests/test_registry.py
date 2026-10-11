import json

import pytest

from app.registry import ModelIntegrityError, ModelNotFoundError, ModelRegistry


def test_load_caches_the_verified_model(trained_artifacts_dir, trained_version):
    registry = ModelRegistry(trained_artifacts_dir)

    first = registry.load(trained_version)

    assert first.version == trained_version
    assert registry.load(trained_version) is first


def test_list_versions_ignores_folders_without_metadata(trained_artifacts_dir, trained_version):
    (trained_artifacts_dir / "scratch").mkdir()

    assert ModelRegistry(trained_artifacts_dir).list_versions() == [trained_version]


@pytest.mark.parametrize("version", ["../etc", "a/b", "", "v1;rm"])
def test_rejects_versions_that_could_escape_the_artifacts_dir(trained_artifacts_dir, version):
    with pytest.raises(ModelNotFoundError):
        ModelRegistry(trained_artifacts_dir).load(version)


def test_legacy_version_without_recorded_hash_is_listed_but_never_served(trained_artifacts_dir, trained_version):
    metadata_file = trained_artifacts_dir / trained_version / "metadata.json"
    metadata = json.loads(metadata_file.read_text())
    del metadata["artifact"]
    metadata_file.write_text(json.dumps(metadata))
    registry = ModelRegistry(trained_artifacts_dir)

    assert registry.describe(trained_version)["artifact_verified"] is False
    with pytest.raises(ModelIntegrityError):
        registry.load(trained_version)
