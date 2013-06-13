package libsidplay.components.mos656x;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Classic octree color quantization algorithm. Given a histogram as packed
 * color value and count of occurences, computes and returns the optimal
 * palette when requested from the state of the tree.
 * <p>
 * The class operates on 24-bit colors. The components are called red, green
 * and blue, but they can be anything. The components should be in some coding
 * that compensates for eye's different sensitivity for errors in each component,
 * as the color distance calculation and quantization assumes so.
 * 
 * @author alankila
 */
class OctreeQuantization {
	/** Bits per component */
	private static final int MAX_DEPTH = 10;

	/** Bits per component in color lookup bucket */
	private static final int MAX_BUCKET_DEPTH = 2;
	
	private static final int COMPONENT_MASK = (1 << MAX_DEPTH) - 1;

	/** Fast color lookup in least-squares sense- Each color is put in bucket. */
	final List<List<Node>> buckets = new ArrayList<List<Node>>();
	
	/** The start of quantization tree. */
	final Node root = new Node(null);

	/** List of current leaf nodes. leaf.parents are targets for reduction. */
	protected final Set<Node> leaves = new HashSet<Node>();

	/** Size of palette to compute. */
	private final int max;

	/**
	 * Quantizer instance. Make a new quantizer for specific number of colors,
	 * add colors, and finally ask for palette.
	 * 
	 * @param max size of palette to quantize to.
	 */
	protected OctreeQuantization(final int max) {
		this.max = max;
	}

	private class Node {
		private boolean leaf;
		protected int reference;
		protected int red;
		protected int green;
		protected int blue;
		protected final Node parent;

		private final Node[] children = new Node[8];

		protected Node(final Node parent) {
			this.parent = parent;
		}

		/**
		 * Select appropriate child node based on requested depth
		 * 
		 * @param packed
		 * @param depth
		 * @return child index
		 */
		private final int pickChild(int packed, int depth) {
			int r = (packed >> (MAX_DEPTH * 2 + depth - 1)) & 1;
			int g = (packed >> (MAX_DEPTH * 1 + depth - 1)) & 1;
			int b = (packed >> (MAX_DEPTH * 0 + depth - 1)) & 1;
			return r << 2 | g << 1 | b;
		}
		
		/**
		 * Add a weighed pixel into tree.
		 *
		 * @param packed the packed data to add.
		 * @param weight the weight of this pixel
		 * @param depth current depth
		 */
		protected void add(final int packed, final int weight, final int depth) {
			reference += weight;

			if (leaf) {
				red +=   (packed >> (MAX_DEPTH * 2) & COMPONENT_MASK) * weight;
				green += (packed >> (MAX_DEPTH * 1) & COMPONENT_MASK) * weight;
				blue +=  (packed >> (MAX_DEPTH * 0) & COMPONENT_MASK) * weight;
				return;
			}

			final int i = pickChild(packed, depth);
			if (children[i] == null) {
				children[i] = new Node(this);
				if (depth == 1) {
					leaves.add(children[i]);
					children[i].leaf = true;
				}
			}

			children[i].add(packed, weight, depth - 1);
		}

		/**
		 * Find nearest node in tree to color
		 * 
		 * @param packed the color to look for
		 * @param depth current depth
		 * @return nearest node
		 */
		@SuppressWarnings("unused")
		protected Node find(final int packed, final int depth) {
			final int i = pickChild(packed, depth);

			/* Further nodes in tree? Enter. */
			if (children[i] != null) {
				return children[i].find(packed, depth - 1);
			}

			/* In a general case we might end up in situation where this node is not
			 * a leaf node. However, because we only ask after colors that are added
			 * to the tree, we will always be a leaf here. */
			return this;
		}
		
		/**
		 * Reduce a node and mark it as leaf.
		 * <p>
		 * Invariant: because this only collapses child nodes into itself,
		 * reference count of parent remains unchanged.
		 * <p>
		 * Because we are calling reduce always for the least referenced parent,
		 * every parent candidate for reduction can only have leaf nodes under it.
		 * <p>
		 */
		protected void reduce() {
			/* collapse all subnodes into this node. */
			for (final Node c : children) {
				if (c == null) {
					continue;
				}

				red += c.red;
				green += c.green;
				blue += c.blue;

				leaves.remove(c);
			}

			leaf = true;
			leaves.add(this);

			Arrays.fill(children, null);
		}

		/**
		 * Construct original kind of packed color value from the node.
		 * 
		 * @return packed color.
		 */
		protected int toPacked() {
			final int r = red / reference;
			final int g = green / reference;
			final int b = blue / reference;
			return r << MAX_DEPTH * 2 | g << MAX_DEPTH * 1 | b << MAX_DEPTH * 0;
		}

		/**
		 * Calculate distance in 3d geometric terms between two colors.
		 * YUV palette is used: it should be perceptively linear coding system.
		 * 
		 * @param a
		 * @return distance squared.
		 */
		protected int distance(final Node a) {
			final int dr = a.red / a.reference - red / reference;
			final int dg = a.green / a.reference - green / reference;
			final int db = a.blue / a.reference - blue / reference;
			return dr * dr + dg * dg + db * db;
		}
	}

	/**
	 * Add a color to quantization algorithm.
	 * 
	 * @param color The packed color to add.
	 * @param weight Number of occurences of this color. (How important it is.)
	 */
	protected void addColor(final int color, final int weight) {
		root.add(color, weight, MAX_DEPTH);
	}

	/**
	 * Actually quantize the palette.
	 * 
	 * Called when a palette is requested.
	 */
	private void quantize() {
		if (leaves.size() <= max) {
			return;
		}

		/* Create a sorted tree that can be used to select the least useful
		 * colors.
		 */
		final TreeSet<Node> parentSet = new TreeSet<Node>(new Comparator<Node>() {
			public int compare(final Node o1, final Node o2) {
				/* Tree is sorted according to usage count and secondarily by
				 * id to disambiguate equally used colors.
				 */
				if (o1.reference > o2.reference) {
					return 1;
				}
				if (o1.reference < o2.reference) {
					return -1;
				}

				final int id1 = System.identityHashCode(o1);
				final int id2 = System.identityHashCode(o2);

				if (id1 > id2) {
					return 1;
				}
				if (id1 < id2) {
					return -1;
				}

				return 0;
			}
		});

		for (final Node leaf : leaves) {
			parentSet.add(leaf.parent);
		}

		/* Reduce nodes until we have dispensed with enough colors. */
		while (leaves.size() > max) {
			final Node parent = parentSet.first();
			parentSet.remove(parent);
			parent.reduce();
			parentSet.add(parent.parent);
		}
		
	}
	
	/**
	 * Returns the optimized palette of constructor-time specified length.
	 * Pads with 0 if more colors were requested than existed in source material.
	 * 
	 * @return
	 */
	protected int[] getPalette() {
		quantize();

		/* Construct accelerator structure */
		buckets.clear();
		for (int i = 0; i < 1 << 3 * MAX_BUCKET_DEPTH; i ++) {
			buckets.add(new ArrayList<Node>());
		}

		int i = 0;
		final int[] finalPalette = new int[max];
		for (final Node node : leaves) {
			finalPalette[i ++] = node.toPacked();
			bucketStore(node);
		}

		return finalPalette;
	}

	/**
	 * Generate color buckets for faster color lookup. Each color
	 * is distributed to the buckets at granularity of 2 bits
	 * per component (4 * 4 * 4 = 64 buckets in total).
	 * 
	 * @param node
	 */
	private void bucketStore(Node node) {
		int color = node.toPacked();
		/* MAX_DEPTH bits */
		int r_ref = (color >> MAX_DEPTH * 2) & COMPONENT_MASK;
		int g_ref = (color >> MAX_DEPTH * 1) & COMPONENT_MASK;
		int b_ref = (color >> MAX_DEPTH * 0) & COMPONENT_MASK;
	
		/* Reduced to 2 bits now. */
		r_ref >>= MAX_DEPTH - MAX_BUCKET_DEPTH;
		g_ref >>= MAX_DEPTH - MAX_BUCKET_DEPTH;
		b_ref >>= MAX_DEPTH - MAX_BUCKET_DEPTH;
		
		/* Scan the cube around the reference value */
		for (int dr = -1; dr <= 0; dr += 1) {
			for (int dg = -1; dg <= 0; dg += 1) {
				for (int db = -1; db <= 0; db += 1) {
					int r = r_ref + dr;
					int g = g_ref + dg;
					int b = b_ref + db;
					
					/* Exclude outside buckets */
					if (r < 0 || g < 0 || b < 0) {
						continue;
					}
					if (r > 7 || g > 7 || b > 7) {
						continue;
					}

					int bucketIdx = r << MAX_BUCKET_DEPTH * 2 | g << MAX_BUCKET_DEPTH * 1 | b << MAX_BUCKET_DEPTH * 0;
					buckets.get(bucketIdx).add(node);
				}
			}
		}
	}

	/**
	 * Do a nearest-node scan looking for best match.
	 * 
	 * @param color
	 * @return best node from simple scan
	 */
	public int lookup(final int color) {
		/* MAX_DEPTH bits */
		int r_ref = (color >> MAX_DEPTH * 2) & COMPONENT_MASK;
		int g_ref = (color >> MAX_DEPTH * 1) & COMPONENT_MASK;
		int b_ref = (color >> MAX_DEPTH * 0) & COMPONENT_MASK;
	
		/* Reduced to 3 bits now. */
		int rb = r_ref >> MAX_DEPTH - MAX_BUCKET_DEPTH;
		int gb = g_ref >> MAX_DEPTH - MAX_BUCKET_DEPTH;
		int bb = b_ref >> MAX_DEPTH - MAX_BUCKET_DEPTH;
		int bucketIdx = rb << MAX_BUCKET_DEPTH * 2 | gb << MAX_BUCKET_DEPTH * 1 | bb << MAX_BUCKET_DEPTH * 0;
		Iterable<Node> bucket = buckets.get(bucketIdx);
		if (! bucket.iterator().hasNext()) {
			/* Nothing nearby. Hopefully this doesn't happen often. */
			bucket = leaves;
		}
		
		int bestDistance = Integer.MAX_VALUE;
		Node bestMatch = null;

		Node n = new Node(null);
		n.red = r_ref;
		n.green = g_ref;
		n.blue = b_ref;
		n.reference = 1;
		for (Node s : bucket) {
			int trialDistance = n.distance(s);
			if (trialDistance < bestDistance) {
				bestDistance = trialDistance;
				bestMatch = s;
			}
		}

		assert(bestMatch != null);
		//Node fast = root.find(color, MAX_DEPTH);		
		//System.out.println("All that work paid off by: " + fast.distance(bestMatch));
		return bestMatch.toPacked();
	}
}
