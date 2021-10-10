package x.mvmn.sonivm.ui.model;

public enum RepeatMode {
	OFF, PLAYLIST, ARTIST, ALBUM, TRACK;

	public String toString() {
		String name = this.name();

		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}
}
