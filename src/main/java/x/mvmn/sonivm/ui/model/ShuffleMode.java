package x.mvmn.sonivm.ui.model;

public enum ShuffleMode {
	OFF, PLAYLIST, ARTIST, ALBUM;

	public String toString() {
		String name = this.name();

		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}
}
