# sonivm
Sonivm audio player

Desktop GUI audio player, based on these amazing FOSS LGPL libraries 
- Audio: FFSampledSP https://www.tagtraum.com/ffsampledsp/ (which is based on FFMPEG)
- Tags: JAudioTagger http://www.jthink.net/jaudiotagger/
- Equalizer: JEQ https://github.com/whamtet/jeq / https://sourceforge.net/projects/jeq/

Features:
- Support of file formats handled by FFAudioService (bundled with native ffmpeg for macOS and Windows)
- Tag reading
- 10-band graphic equalizer (with presets management and possibility to export/import WinAmp EqF file format)
- LastFM
- CUE files
- WinAmp skins
- Gapeless playback for single file tracks from CUE sheet (no gapless for separate files yet)
- Drag&drop files into play-queue, drag&drop tracks within play-queue
- Smart repeat
- Smart shuffle (with a smarter party shuffle coming later)
- Search in play queue
- Minimize to System Tray

Planned features:
- Music library management
- Advanced “party-shuffle” modes, like year-range based shuffle, rating based shuffle etc. The rating based one would require functionality to rate tracks of course
- Guess the music game (where a random track from music library is played, and one has to guess from a number of options which artist/album/track it is)
- Gapeless playback for individual files
- (If I can pull if off) Support of WinAmp 2 skins AKA “classic skins”
- Other minor improvements (like saving/restoring UI location/size between restarts)

Requires Java 8 (AKA Java 1.8) or above
