# sonivm
Sonivm audio player

Desktop GUI audio player, based on https://www.tagtraum.com/ffsampledsp/ and http://www.jthink.net/jaudiotagger/

Features:
- Support of file formats handled by FFAudioService (bundled with native ffmpeg for macOS and Windows)
- Tag reading support
- CUE files support
- Gapeless playback for single file tracks from CUE sheet (no gapless for separate files yet)
- LastFM scrobbling support
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
