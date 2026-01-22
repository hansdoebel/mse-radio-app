import { query } from "./_generated/server";
import { v } from "convex/values";

export const list = query({
  args: {},
  handler: async (ctx) => {
    return await ctx.db.query("playlists").collect();
  },
});

export const getSongsInPlaylist = query({
  args: { playlistId: v.id("playlists") },
  handler: async (ctx, args) => {
    const playlistSongs = await ctx.db
      .query("playlistSongs")
      .withIndex("by_playlist", (q) => q.eq("playlistId", args.playlistId))
      .collect();

    const songs = await Promise.all(
      playlistSongs.map(async (ps) => {
        const song = await ctx.db.get(ps.songId);
        return song;
      }),
    );

    return songs.filter((s) => s !== null);
  },
});
