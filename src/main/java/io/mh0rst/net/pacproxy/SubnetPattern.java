/*
 * PacProxy - A HTTP proxy driven by a PAC file.
 * Copyright (C) 2017 Moritz Horstmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.mh0rst.net.pacproxy;

/**
 * Detects if an address is part of the subnet represented by this class.
 */
public class SubnetPattern {

    int[] subnetMask;

    int[] mask;

    /**
     * Creates a new subnet representation using the syntax of PAC function isInNet(host, pattern, mask) in bytes.
     * 
     * @param subnetPatternBytes
     * @param maskBytes
     */
    public SubnetPattern(byte[] subnetPatternBytes, byte[] maskBytes) {
        int length = maskBytes.length / 4;
        if (subnetPatternBytes.length / 4 != length) {
            throw new IllegalArgumentException("Given subnet does not match length of mask");
        }
        subnetMask = new int[length];
        mask = new int[length];
        for (int maskPart = 0; maskPart < length; maskPart++) {
            mask[maskPart] = bytesToInt(maskBytes, maskPart);
            subnetMask[maskPart] = bytesToInt(subnetPatternBytes, maskPart);
            subnetMask[maskPart] &= mask[maskPart];
        }
    }

    private int bytesToInt(byte[] in, int offset) {
        return ((in[0 + (4 * offset)] & 0xFF) << 24) | ((in[1 + (4 * offset)] & 0xFF) << 16) |
               ((in[2 + (4 * offset)] & 0xFF) << 8) | ((in[3 + (4 * offset)] & 0xFF) << 0);
    }

    /**
     * Returns true if the given address is part of the subnet.
     * 
     * @param address
     */
    public boolean isInMask(byte[] address) {
        if (address.length / 4 != subnetMask.length) {
            throw new IllegalArgumentException("Given address does not match length of mask");
        }
        for (int maskPart = 0; maskPart < subnetMask.length; maskPart++) {
            int addressPart = bytesToInt(address, maskPart);
            if ((addressPart & mask[maskPart]) != subnetMask[maskPart]) {
                return false;
            }
        }
        return true;
    }
}
