import { mutation, query } from "./_generated/server";
import { v } from "convex/values";

export const whoami = query({
  args: {},
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) return null;

    return await ctx.db
      .query("users")
      .withIndex(
        "by_token",
        (q) => q.eq("tokenIdentifier", identity.tokenIdentifier),
      )
      .first();
  },
});

export const upsert = mutation({
  args: {
    name: v.string(),
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) {
      throw new Error("Not authenticated");
    }

    const existingUser = await ctx.db
      .query("users")
      .withIndex(
        "by_token",
        (q) => q.eq("tokenIdentifier", identity.tokenIdentifier),
      )
      .first();

    if (existingUser) {
      await ctx.db.patch(existingUser._id, {
        name: args.name,
      });
      return existingUser._id;
    } else {
      return await ctx.db.insert("users", {
        email: identity.email ?? "",
        name: args.name,
        role: "listener",
        tokenIdentifier: identity.tokenIdentifier,
      });
    }
  },
});
