import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  users: defineTable({
    email: v.string(),
    name: v.string(),
    role: v.union(v.literal("listener"), v.literal("moderator")),
    tokenIdentifier: v.string(),
  })
    .index("by_token", ["tokenIdentifier"])
    .index("by_email", ["email"]),

  playlists: defineTable({
    name: v.string(),
    description: v.optional(v.string()),
  }).index("by_name", ["name"]),

  songs: defineTable({
    title: v.string(),
    artist: v.string(),
    album: v.string(),
    durationMs: v.number(),
  })
    .index("by_artist", ["artist"])
    .index("by_title", ["title"]),

  playlistSongs: defineTable({
    playlistId: v.id("playlists"),
    songId: v.id("songs"),
  })
    .index("by_playlist", ["playlistId"])
    .index("by_song", ["songId"]),

  nowPlaying: defineTable({
    songId: v.id("songs"),
    playlistId: v.optional(v.id("playlists")),
    moderatorId: v.optional(v.id("users")),
    startedAt: v.number(),
    isPlaying: v.optional(v.boolean()),
    pausedAt: v.optional(v.number()),
  }),

  ratings: defineTable({
    userId: v.id("users"),
    targetId: v.string(),
    targetType: v.union(
      v.literal("playlist"),
      v.literal("moderator"),
    ),
    value: v.number(),
    comment: v.optional(v.string()),
  })
    .index("by_user", ["userId"])
    .index("by_target", ["targetId", "targetType"])
    .index("by_user_and_target", ["userId", "targetId", "targetType"]),

  songRequests: defineTable({
    userId: v.id("users"),
    songTitle: v.string(),
    artistName: v.optional(v.string()),
    status: v.union(
      v.literal("pending"),
      v.literal("approved"),
      v.literal("rejected"),
      v.literal("played"),
    ),
    processedAt: v.optional(v.number()),
  })
    .index("by_user", ["userId"])
    .index("by_status", ["status"]),
});
