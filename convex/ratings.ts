import { mutation, query } from "./_generated/server";
import { v } from "convex/values";

export const submitRating = mutation({
  args: {
    targetId: v.string(),
    targetType: v.union(v.literal("playlist"), v.literal("moderator")),
    value: v.union(v.number(), v.int64()),
    comment: v.optional(v.string()),
    userId: v.optional(v.id("users")),
  },
  handler: async (ctx, args) => {
    let user = null;

    if (args.userId) {
      user = await ctx.db.get(args.userId);
    } else {
      const identity = await ctx.auth.getUserIdentity();
      if (identity) {
        user = await ctx.db
          .query("users")
          .withIndex(
            "by_token",
            (q) => q.eq("tokenIdentifier", identity.tokenIdentifier),
          )
          .first();
      }
    }

    const value = Number(args.value);
    if (value < 1 || value > 5) {
      throw new Error("Bewertung muss zwischen 1 und 5 liegen");
    }

    if (!user) {
      throw new Error("Benutzer nicht gefunden");
    }

    const existingRating = await ctx.db
      .query("ratings")
      .withIndex(
        "by_user_and_target",
        (q) =>
          q.eq("userId", user._id).eq("targetId", args.targetId).eq(
            "targetType",
            args.targetType,
          ),
      )
      .first();

    if (existingRating) {
      await ctx.db.patch(existingRating._id, {
        value,
        comment: args.comment,
      });
      return existingRating._id;
    } else {
      return await ctx.db.insert("ratings", {
        userId: user._id,
        targetId: args.targetId,
        targetType: args.targetType,
        value,
        comment: args.comment,
      });
    }
  },
});

export const getStats = query({
  args: {
    targetId: v.string(),
    targetType: v.union(v.literal("playlist"), v.literal("moderator")),
  },
  handler: async (ctx, args) => {
    const ratings = await ctx.db
      .query("ratings")
      .withIndex(
        "by_target",
        (q) =>
          q.eq("targetId", args.targetId).eq("targetType", args.targetType),
      )
      .collect();

    if (ratings.length === 0) {
      return { averageRating: 0, totalRatings: 0 };
    }

    const sum = ratings.reduce((acc, r) => acc + r.value, 0);
    return {
      averageRating: sum / ratings.length,
      totalRatings: ratings.length,
    };
  },
});

export const getRatings = query({
  args: {
    limit: v.optional(v.union(v.number(), v.int64())),
  },
  handler: async (ctx, args) => {
    const limit = Number(args.limit ?? 20);
    const ratings = await ctx.db.query("ratings").order("desc").take(limit);

    const ratingsWithDetails = await Promise.all(
      ratings.map(async (rating) => {
        const user = await ctx.db.get(rating.userId);

        let targetName = "Unbekannt";
        if (rating.targetType === "playlist") {
          const playlistId = ctx.db.normalizeId("playlists", rating.targetId);
          if (playlistId) {
            const playlist = await ctx.db.get(playlistId);
            targetName = playlist?.name ?? "Unbekannte Playlist";
          }
        } else if (rating.targetType === "moderator") {
          const moderatorId = ctx.db.normalizeId("users", rating.targetId);
          if (moderatorId) {
            const moderator = await ctx.db.get(moderatorId);
            targetName = moderator?.name ?? "Unbekannter Moderator";
          }
        }

        return {
          ...rating,
          userName: user?.name ?? "Unbekannt",
          targetName,
        };
      }),
    );

    return ratingsWithDetails;
  },
});
