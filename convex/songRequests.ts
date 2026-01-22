import { mutation, query } from "./_generated/server";
import { v } from "convex/values";

export const submit = mutation({
  args: {
    songTitle: v.string(),
    artistName: v.optional(v.string()),
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

    if (!user) {
      throw new Error("User not found");
    }

    return await ctx.db.insert("songRequests", {
      userId: user._id,
      songTitle: args.songTitle,
      artistName: args.artistName,
      status: "pending",
      processedAt: undefined,
    });
  },
});

export const getMyRequests = query({
  args: {
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

    if (!user) return [];

    return await ctx.db
      .query("songRequests")
      .withIndex("by_user", (q) => q.eq("userId", user._id))
      .order("desc")
      .collect();
  },
});

export const getPendingRequests = query({
  args: {},
  handler: async (ctx) => {
    const requests = await ctx.db
      .query("songRequests")
      .withIndex("by_status", (q) => q.eq("status", "pending"))
      .order("desc")
      .collect();

    const requestsWithUsers = await Promise.all(
      requests.map(async (request) => {
        const requestUser = await ctx.db.get(request.userId);
        return {
          ...request,
          userName: requestUser?.name ?? "Unknown",
        };
      }),
    );

    return requestsWithUsers;
  },
});

export const updateStatus = mutation({
  args: {
    requestId: v.id("songRequests"),
    status: v.union(
      v.literal("approved"),
      v.literal("rejected"),
    ),
  },
  handler: async (ctx, args) => {
    await ctx.db.patch(args.requestId, {
      status: args.status,
      processedAt: Date.now(),
    });
  },
});
