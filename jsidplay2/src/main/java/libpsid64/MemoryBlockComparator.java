package libpsid64;

import java.util.Comparator;

final class MemoryBlockComparator implements Comparator<MemoryBlock> {
	@Override
	public int compare(final MemoryBlock a, final MemoryBlock b) {
		if (a.startAddress < b.startAddress) {
			return -1;
		}
		if (a.startAddress > b.startAddress) {
			return 1;
		}

		return 0;

	}
}