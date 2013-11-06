interface AESinterface{
	public void encrypt(int[] input);
	public void decrypt(int[] input);
	//input size should be 16
}

class AES implements AESinterface{
	AES(int[] input){
		int i, j, k;
		roundKey = new int[block_width*(num_round+1)][];
		state = new int[block_width][block_width];
		for(i=0;i<block_width*block_width;++i){
			if(i%block_width==0)
				roundKey[i/block_width] = new int[block_width];
			roundKey[i/block_width][i%block_width] = input[i];
		}
		//keySchedule
		for(i=0,j=block_width-1;i<block_width*num_round;++i,++j){
			int[] tmp=new int[block_width];
			if((i%block_width)==0){
				//rotWord
				for(k=0;k<block_width;++k)
					tmp[k] = roundKey[j][(k+1)%block_width];
				//subBytes & XOR
				for(k=0;k<block_width;++k)
					tmp[k] = SBox[tmp[k]/16][tmp[k]%16] ^ Rcon[k][i/4] ^ roundKey[i][k];
			}
			else{
				for(k=0;k<block_width;++k)
					tmp[k] = roundKey[i][k] ^ roundKey[j][k];
			}
			roundKey[i+block_width] = tmp;
		}
	}
	public void encrypt(int[] input){
		int i, j, k;
		for(i=k=0;i<block_width;++i)
			for(j=0;j<block_width;++j)
				state[i][j] = input[k++];
		//initial round
		addRoundKey(0);
		//9 rounds
		for(i=1;i<num_round;++i){
			subBytes();
			shiftRows();
			mixCloumns();
			addRoundKey(i);
		}
		//final round
		subBytes();
		shiftRows();
		addRoundKey(num_round);
		//conver to array
		for(i=k=0;i<block_width;++i)
			for(j=0;j<block_width;++j)
				input[k++] = state[i][j];
	}
	public void decrypt(int[] input){
		int i, j, k;
		for(i=k=0;i<block_width;++i)
			for(j=0;j<block_width;++j)
				state[i][j] = input[k++];
		addRoundKey(num_round);
		invShiftRows();
		invSubBytes();
		for(i=num_round-1;i>0;--i){
			addRoundKey(i);
			invMixColumns();
			invShiftRows();
			invSubBytes();
		}
		addRoundKey(0);
		for(i=k=0;i<block_width;++i)
			for(j=0;j<block_width;++j)
				input[k++] = state[i][j];
	}
	private void addRoundKey(int round){
		int i, j, k=block_width*round;
		for(i=k;i<k+block_width;++i)
			for(j=0;j<block_width;++j)
				state[i%block_width][j] = state[i%block_width][j] ^ roundKey[j+k][i%block_width];
	}
	private void subBytes(){
		int tmp, i, j;
		for(i=0;i<block_width;++i)
			for(j=0;j<block_width;++j){
				tmp = state[i][j];
				state[i][j] = SBox[tmp/16][tmp%16];
			}
	}
	private void shiftRows(){
		int i, j;
		int[][] tmp=new int[block_width][block_width];
		for(i=0;i<block_width;++i){
			for(j=0;j<block_width;++j)
				tmp[i][j] = state[i][(i+j)%block_width];
			for(j=0;j<block_width;++j)
				state[i][j] = tmp[i][j];
		}
	}
	private void mixCloumns(){
		int i, j, h;
		int[] a=new int[block_width], b=new int[block_width];
		for(i=0;i<block_width;++i){
			for(j=0;j<block_width;++j){
				a[j] = state[j][i];
				b[j] = (state[j][i] << 1) & 0xFF;
				if((a[j]&0x80)==0)
					h = 0;
				else
					h = 0xFF;
				b[j] ^= 0x1B & h;
			}
			state[0][i] = b[0] ^ a[3] ^ a[2] ^ b[1] ^ a[1];
			state[1][i] = b[1] ^ a[0] ^ a[3] ^ b[2] ^ a[2];
			state[2][i] = b[2] ^ a[1] ^ a[0] ^ b[3] ^ a[3];
			state[3][i] = b[3] ^ a[2] ^ a[1] ^ b[0] ^ a[0];
		}
	}
	private void invSubBytes(){
		int tmp, i, j;
		for(i=0;i<block_width;++i)
			for(j=0;j<block_width;++j){
				tmp = state[i][j];
				state[i][j] = invSBox[tmp/16][tmp%16];
			}
	}
	private void invShiftRows(){
		int i, j;
		int[][] tmp=new int[block_width][block_width];
		for(i=0;i<block_width;++i){
			for(j=0;j<block_width;++j)
				tmp[i][(i+j)%block_width] = state[i][j];
			for(j=0;j<block_width;++j)
				state[i][j] = tmp[i][j];
		}
	}
	private void invMixColumns(){
		int i, j, k, l;
		int[] r=new int[block_width], s=new int[block_width];
		int[] invMix={0x0E, 0x0B, 0x0D, 0x09};
		int[][] tmp=new int[block_width][block_width];
		for(i=0;i<block_width;++i)
			for(j=0;j<block_width;++j)
				tmp[i][j] = state[i][j];
		for(i=0;i<block_width;++i)
			for(j=0;j<block_width;++j){
				for(k=0;k<block_width;++k){
					r[0] = tmp[k][i];
					for(l=1;l<block_width;++l){
						r[l] = (r[l-1] << 1) & 0xFF;
						if((r[l-1]&0x80)!=0)
							r[l] ^= 0x1B;
					}
					switch(invMix[(k-j+4)%4]){
						case 14:
							s[k] = r[1] ^ r[2] ^ r[3];
							break;
						case 13:
							s[k] = r[0] ^ r[2] ^ r[3];
							break;
						case 11:
							s[k] = r[0] ^ r[1] ^ r[3];
							break;
						case 9:
							s[k] = r[0] ^ r[3];
							break;
					}
				}
				state[j][i] = s[0] ^ s[1] ^ s[2] ^ s[3];
			}
	}
	private int[][] state, roundKey;
	private final int block_width=4, num_round=10;
	private final int[][]
	SBox=
	{
		{0x63, 0x7C, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5, 0x30, 0x01, 0x67, 0x2B, 0xFE, 0xD7, 0xAB, 0x76},
		{0xCA, 0x82, 0xC9, 0x7D, 0xFA, 0x59, 0x47, 0xF0, 0xAD, 0xD4, 0xA2, 0xAF, 0x9C, 0xA4, 0x72, 0xC0},
		{0xB7, 0xFD, 0x93, 0x26, 0x36, 0x3F, 0xF7, 0xCC, 0x34, 0xA5, 0xE5, 0xF1, 0x71, 0xD8, 0x31, 0x15},
		{0x04, 0xC7, 0x23, 0xC3, 0x18, 0x96, 0x05, 0x9A, 0x07, 0x12, 0x80, 0xE2, 0xEB, 0x27, 0xB2, 0x75},
		{0x09, 0x83, 0x2C, 0x1A, 0x1B, 0x6E, 0x5A, 0xA0, 0x52, 0x3B, 0xD6, 0xB3, 0x29, 0xE3, 0x2F, 0x84},
		{0x53, 0xD1, 0x00, 0xED, 0x20, 0xFC, 0xB1, 0x5B, 0x6A, 0xCB, 0xBE, 0x39, 0x4A, 0x4C, 0x58, 0xCF},
		{0xD0, 0xEF, 0xAA, 0xFB, 0x43, 0x4D, 0x33, 0x85, 0x45, 0xF9, 0x02, 0x7F, 0x50, 0x3C, 0x9F, 0xA8},
		{0x51, 0xA3, 0x40, 0x8F, 0x92, 0x9D, 0x38, 0xF5, 0xBC, 0xB6, 0xDA, 0x21, 0x10, 0xFF, 0xF3, 0xD2},
		{0xCD, 0x0C, 0x13, 0xEC, 0x5F, 0x97, 0x44, 0x17, 0xC4, 0xA7, 0x7E, 0x3D, 0x64, 0x5D, 0x19, 0x73},
		{0x60, 0x81, 0x4F, 0xDC, 0x22, 0x2A, 0x90, 0x88, 0x46, 0xEE, 0xB8, 0x14, 0xDE, 0x5E, 0x0B, 0xDB},
		{0xE0, 0x32, 0x3A, 0x0A, 0x49, 0x06, 0x24, 0x5C, 0xC2, 0xD3, 0xAC, 0x62, 0x91, 0x95, 0xE4, 0x79},
		{0xE7, 0xC8, 0x37, 0x6D, 0x8D, 0xD5, 0x4E, 0xA9, 0x6C, 0x56, 0xF4, 0xEA, 0x65, 0x7A, 0xAE, 0x08},
		{0xBA, 0x78, 0x25, 0x2E, 0x1C, 0xA6, 0xB4, 0xC6, 0xE8, 0xDD, 0x74, 0x1F, 0x4B, 0xBD, 0x8B, 0x8A},
		{0x70, 0x3E, 0xB5, 0x66, 0x48, 0x03, 0xF6, 0x0E, 0x61, 0x35, 0x57, 0xB9, 0x86, 0xC1, 0x1D, 0x9E},
		{0xE1, 0xF8, 0x98, 0x11, 0x69, 0xD9, 0x8E, 0x94, 0x9B, 0x1E, 0x87, 0xE9, 0xCE, 0x55, 0x28, 0xDF},
		{0x8C, 0xA1, 0x89, 0x0D, 0xBF, 0xE6, 0x42, 0x68, 0x41, 0x99, 0x2D, 0x0F, 0xB0, 0x54, 0xBB, 0x16}
	},
	invSBox=
	{
		{0x52, 0x09, 0x6A, 0xD5, 0x30, 0x36, 0xA5, 0x38, 0xBF, 0x40, 0xA3, 0x9E, 0x81, 0xF3, 0xD7, 0xFB},
		{0x7C, 0xE3, 0x39, 0x82, 0x9B, 0x2F, 0xFF, 0x87, 0x34, 0x8E, 0x43, 0x44, 0xC4, 0xDE, 0xE9, 0xCB},
		{0x54, 0x7B, 0x94, 0x32, 0xA6, 0xC2, 0x23, 0x3D, 0xEE, 0x4C, 0x95, 0x0B, 0x42, 0xFA, 0xC3, 0x4E},
		{0x08, 0x2E, 0xA1, 0x66, 0x28, 0xD9, 0x24, 0xB2, 0x76, 0x5B, 0xA2, 0x49, 0x6D, 0x8B, 0xD1, 0x25},
		{0x72, 0xF8, 0xF6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xD4, 0xA4, 0x5C, 0xCC, 0x5D, 0x65, 0xB6, 0x92},
		{0x6C, 0x70, 0x48, 0x50, 0xFD, 0xED, 0xB9, 0xDA, 0x5E, 0x15, 0x46, 0x57, 0xA7, 0x8D, 0x9D, 0x84},
		{0x90, 0xD8, 0xAB, 0x00, 0x8C, 0xBC, 0xD3, 0x0A, 0xF7, 0xE4, 0x58, 0x05, 0xB8, 0xB3, 0x45, 0x06},
		{0xD0, 0x2C, 0x1E, 0x8F, 0xCA, 0x3F, 0x0F, 0x02, 0xC1, 0xAF, 0xBD, 0x03, 0x01, 0x13, 0x8A, 0x6B},
		{0x3A, 0x91, 0x11, 0x41, 0x4F, 0x67, 0xDC, 0xEA, 0x97, 0xF2, 0xCF, 0xCE, 0xF0, 0xB4, 0xE6, 0x73},
		{0x96, 0xAC, 0x74, 0x22, 0xE7, 0xAD, 0x35, 0x85, 0xE2, 0xF9, 0x37, 0xE8, 0x1C, 0x75, 0xDF, 0x6E},
		{0x47, 0xF1, 0x1A, 0x71, 0x1D, 0x29, 0xC5, 0x89, 0x6F, 0xB7, 0x62, 0x0E, 0xAA, 0x18, 0xBE, 0x1B},
		{0xFC, 0x56, 0x3E, 0x4B, 0xC6, 0xD2, 0x79, 0x20, 0x9A, 0xDB, 0xC0, 0xFE, 0x78, 0xCD, 0x5A, 0xF4},
		{0x1F, 0xDD, 0xA8, 0x33, 0x88, 0x07, 0xC7, 0x31, 0xB1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xEC, 0x5F},
		{0x60, 0x51, 0x7F, 0xA9, 0x19, 0xB5, 0x4A, 0x0D, 0x2D, 0xE5, 0x7A, 0x9F, 0x93, 0xC9, 0x9C, 0xEF},
		{0xA0, 0xE0, 0x3B, 0x4D, 0xAE, 0x2A, 0xF5, 0xB0, 0xC8, 0xEB, 0xBB, 0x3C, 0x83, 0x53, 0x99, 0x61},
		{0x17, 0x2B, 0x04, 0x7E, 0xBA, 0x77, 0xD6, 0x26, 0xE1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0C, 0x7D}
	},
	Rcon=
	{
		{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36},
		{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
		{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
		{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}
	};
}
