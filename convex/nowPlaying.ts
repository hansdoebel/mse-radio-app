import { mutation, query } from "./_generated/server";
import { v } from "convex/values";

export const get = query({
  args: {},
  handler: async (ctx) => {
    const nowPlaying = await ctx.db.query("nowPlaying").first();
    if (!nowPlaying) return null;

    const song = await ctx.db.get(nowPlaying.songId);
    if (!song) return null;

    const playlist = nowPlaying.playlistId
      ? await ctx.db.get(nowPlaying.playlistId)
      : null;

    let moderator = null;
    if (nowPlaying.moderatorId) {
      moderator = await ctx.db.get(nowPlaying.moderatorId);
    }

    return {
      song: {
        _id: song._id,
        title: song.title,
        artist: song.artist,
        album: song.album,
        durationMs: song.durationMs,
      },
      playlist: playlist
        ? {
          _id: playlist._id,
          name: playlist.name,
          description: playlist.description,
        }
        : null,
      startedAt: nowPlaying.startedAt,
      isPlaying: nowPlaying.isPlaying ?? true,
      pausedAt: nowPlaying.pausedAt,
      moderator: moderator
        ? {
          _id: moderator._id,
          name: moderator.name,
        }
        : null,
    };
  },
});

export const update = mutation({
  args: {
    songId: v.id("songs"),
    playlistId: v.id("playlists"),
    moderatorId: v.optional(v.id("users")),
  },
  handler: async (ctx, args) => {
    const existing = await ctx.db.query("nowPlaying").first();

    if (existing) {
      await ctx.db.patch(existing._id, {
        songId: args.songId,
        playlistId: args.playlistId,
        moderatorId: args.moderatorId,
        startedAt: Date.now(),
        isPlaying: true,
        pausedAt: undefined,
      });
    } else {
      await ctx.db.insert("nowPlaying", {
        songId: args.songId,
        playlistId: args.playlistId,
        moderatorId: args.moderatorId,
        startedAt: Date.now(),
        isPlaying: true,
      });
    }
  },
});

export const pause = mutation({
  args: {},
  handler: async (ctx) => {
    const nowPlaying = await ctx.db.query("nowPlaying").first();
    if (!nowPlaying) return;

    await ctx.db.patch(nowPlaying._id, {
      isPlaying: false,
      pausedAt: Date.now(),
    });
  },
});

export const resume = mutation({
  args: {},
  handler: async (ctx) => {
    const nowPlaying = await ctx.db.query("nowPlaying").first();
    if (!nowPlaying) return;

    const pausedAt = nowPlaying.pausedAt ?? Date.now();
    const pauseDuration = Date.now() - pausedAt;
    const newStartedAt = nowPlaying.startedAt + pauseDuration;

    await ctx.db.patch(nowPlaying._id, {
      isPlaying: true,
      startedAt: newStartedAt,
      pausedAt: undefined,
    });
  },
});

export const skipNext = mutation({
  args: {},
  handler: async (ctx) => {
    const nowPlaying = await ctx.db.query("nowPlaying").first();
    if (!nowPlaying || !nowPlaying.playlistId) return;

    const playlistSongs = await ctx.db
      .query("playlistSongs")
      .withIndex(
        "by_playlist",
        (q) => q.eq("playlistId", nowPlaying.playlistId!),
      )
      .collect();

    if (playlistSongs.length === 0) return;

    const currentIndex = playlistSongs.findIndex(
      (ps) => ps.songId === nowPlaying.songId,
    );
    const nextIndex = (currentIndex + 1) % playlistSongs.length;
    const nextSongId = playlistSongs[nextIndex].songId;

    await ctx.db.patch(nowPlaying._id, {
      songId: nextSongId,
      startedAt: Date.now(),
    });
  },
});

export const skipPrevious = mutation({
  args: {},
  handler: async (ctx) => {
    const nowPlaying = await ctx.db.query("nowPlaying").first();
    if (!nowPlaying || !nowPlaying.playlistId) return;

    const playlistSongs = await ctx.db
      .query("playlistSongs")
      .withIndex(
        "by_playlist",
        (q) => q.eq("playlistId", nowPlaying.playlistId!),
      )
      .collect();

    if (playlistSongs.length === 0) return;

    const currentIndex = playlistSongs.findIndex(
      (ps) => ps.songId === nowPlaying.songId,
    );
    const prevIndex = currentIndex <= 0
      ? playlistSongs.length - 1
      : currentIndex - 1;
    const prevSongId = playlistSongs[prevIndex].songId;

    await ctx.db.patch(nowPlaying._id, {
      songId: prevSongId,
      startedAt: Date.now(),
    });
  },
});

export const clear = mutation({
  args: {},
  handler: async (ctx) => {
    const nowPlaying = await ctx.db.query("nowPlaying").first();
    if (nowPlaying) {
      await ctx.db.delete(nowPlaying._id);
    }
  },
});
