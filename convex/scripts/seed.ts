import { mutation } from "../_generated/server";

export default mutation({
  args: {},
  handler: async (ctx) => {
    const existingPlaylists = await ctx.db.query("playlists").collect();
    if (existingPlaylists.length > 0) {
      return "Database already seeded";
    }

    const playlist1 = await ctx.db.insert("playlists", {
      name: "Pop Hits",
      description: "Die besten Pop-Songs",
    });

    const playlist2 = await ctx.db.insert("playlists", {
      name: "Rock Classics",
      description: "Klassische Rock-Songs",
    });

    const playlist3 = await ctx.db.insert("playlists", {
      name: "Chill Vibes",
      description: "Entspannte Musik zum Relaxen",
    });

    const songs = [
      {
        title: "Blinding Lights",
        artist: "The Weeknd",
        album: "After Hours",
        durationMs: 200000,
      },
      {
        title: "Shape of You",
        artist: "Ed Sheeran",
        album: "Divide",
        durationMs: 234000,
      },
      {
        title: "Dance Monkey",
        artist: "Tones and I",
        album: "The Kids Are Coming",
        durationMs: 210000,
      },
      {
        title: "Bohemian Rhapsody",
        artist: "Queen",
        album: "A Night at the Opera",
        durationMs: 355000,
      },
      {
        title: "Hotel California",
        artist: "Eagles",
        album: "Hotel California",
        durationMs: 391000,
      },
      {
        title: "Sweet Child O Mine",
        artist: "Guns N Roses",
        album: "Appetite for Destruction",
        durationMs: 356000,
      },
      {
        title: "Weightless",
        artist: "Marconi Union",
        album: "Weightless",
        durationMs: 480000,
      },
      {
        title: "Clair de Lune",
        artist: "Debussy",
        album: "Suite Bergamasque",
        durationMs: 300000,
      },
      {
        title: "Sunset Lover",
        artist: "Petit Biscuit",
        album: "Presence",
        durationMs: 237000,
      },
    ];

    const songIds = await Promise.all(
      songs.map((song) => ctx.db.insert("songs", song)),
    );

    await ctx.db.insert("playlistSongs", {
      playlistId: playlist1,
      songId: songIds[0],
    });
    await ctx.db.insert("playlistSongs", {
      playlistId: playlist1,
      songId: songIds[1],
    });
    await ctx.db.insert("playlistSongs", {
      playlistId: playlist1,
      songId: songIds[2],
    });

    await ctx.db.insert("playlistSongs", {
      playlistId: playlist2,
      songId: songIds[3],
    });
    await ctx.db.insert("playlistSongs", {
      playlistId: playlist2,
      songId: songIds[4],
    });
    await ctx.db.insert("playlistSongs", {
      playlistId: playlist2,
      songId: songIds[5],
    });

    await ctx.db.insert("playlistSongs", {
      playlistId: playlist3,
      songId: songIds[6],
    });
    await ctx.db.insert("playlistSongs", {
      playlistId: playlist3,
      songId: songIds[7],
    });
    await ctx.db.insert("playlistSongs", {
      playlistId: playlist3,
      songId: songIds[8],
    });

    return "Seeded 3 playlists and 9 songs";
  },
});
