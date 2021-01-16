package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     DMPMap.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


public interface DMPMap {
	static final int DMP_PTAT    = 0;
	static final int DMP_XGYR    = 2;
	static final int DMP_YGYR    = 4;
	static final int DMP_ZGYR    = 6;
	static final int DMP_XACC    = 8;
	static final int DMP_YACC    = 10;
	static final int DMP_ZACC    = 12;
	static final int DMP_ADC1    = 14;
	static final int DMP_ADC2    = 16;
	static final int DMP_ADC3    = 18;
	static final int DMP_BIASUNC    = 20;
	static final int DMP_FIFORT    = 22;
	static final int DMP_INVGSFH    = 24;
	static final int DMP_INVGSFL    = 26;
	static final int DMP_1H    = 28;
	static final int DMP_1L    = 30;
	static final int DMP_BLPFSTCH    = 32;
	static final int DMP_BLPFSTCL    = 34;
	static final int DMP_BLPFSXH    = 36;
	static final int DMP_BLPFSXL    = 38;
	static final int DMP_BLPFSYH    = 40;
	static final int DMP_BLPFSYL    = 42;
	static final int DMP_BLPFSZH    = 44;
	static final int DMP_BLPFSZL    = 46;
	static final int DMP_BLPFMTC    = 48;
	static final int DMP_SMC    = 50;
	static final int DMP_BLPFMXH    = 52;
	static final int DMP_BLPFMXL    = 54;
	static final int DMP_BLPFMYH    = 56;
	static final int DMP_BLPFMYL    = 58;
	static final int DMP_BLPFMZH    = 60;
	static final int DMP_BLPFMZL    = 62;
	static final int DMP_BLPFC    = 64;
	static final int DMP_SMCTH    = 66;
	static final int DMP_0H2    = 68;
	static final int DMP_0L2    = 70;
	static final int DMP_BERR2H    = 72;
	static final int DMP_BERR2L    = 74;
	static final int DMP_BERR2NH    = 76;
	static final int DMP_SMCINC    = 78;
	static final int DMP_ANGVBXH    = 80;
	static final int DMP_ANGVBXL    = 82;
	static final int DMP_ANGVBYH    = 84;
	static final int DMP_ANGVBYL    = 86;
	static final int DMP_ANGVBZH    = 88;
	static final int DMP_ANGVBZL    = 90;
	static final int DMP_BERR1H    = 92;
	static final int DMP_BERR1L    = 94;
	static final int DMP_ATCH    = 96;
	static final int DMP_BIASUNCSF    = 98;
	static final int DMP_ACT2H    = 100;
	static final int DMP_ACT2L    = 102;
	static final int DMP_GSFH    = 104;
	static final int DMP_GSFL    = 106;
	static final int DMP_GH    = 108;
	static final int DMP_GL    = 110;
	static final int DMP_0_5H    = 112;
	static final int DMP_0_5L    = 114;
	static final int DMP_0_0H    = 116;
	static final int DMP_0_0L    = 118;
	static final int DMP_1_0H    = 120;
	static final int DMP_1_0L    = 122;
	static final int DMP_1_5H    = 124;
	static final int DMP_1_5L    = 126;
	static final int DMP_TMP1AH    = 128;
	static final int DMP_TMP1AL    = 130;
	static final int DMP_TMP2AH    = 132;
	static final int DMP_TMP2AL    = 134;
	static final int DMP_TMP3AH    = 136;
	static final int DMP_TMP3AL    = 138;
	static final int DMP_TMP4AH    = 140;
	static final int DMP_TMP4AL    = 142;
	static final int DMP_XACCW    = 144;
	static final int DMP_TMP5    = 146;
	static final int DMP_XACCB    = 148;
	static final int DMP_TMP8    = 150;
	static final int DMP_YACCB    = 152;
	static final int DMP_TMP9    = 154;
	static final int DMP_ZACCB    = 156;
	static final int DMP_TMP10    = 158;
	static final int DMP_DZH    = 160;
	static final int DMP_DZL    = 162;
	static final int DMP_XGCH    = 164;
	static final int DMP_XGCL    = 166;
	static final int DMP_YGCH    = 168;
	static final int DMP_YGCL    = 170;
	static final int DMP_ZGCH    = 172;
	static final int DMP_ZGCL    = 174;
	static final int DMP_YACCW    = 176;
	static final int DMP_TMP7    = 178;
	static final int DMP_AFB1H    = 180;
	static final int DMP_AFB1L    = 182;
	static final int DMP_AFB2H    = 184;
	static final int DMP_AFB2L    = 186;
	static final int DMP_MAGFBH    = 188;
	static final int DMP_MAGFBL    = 190;
	static final int DMP_QT1H    = 192;
	static final int DMP_QT1L    = 194;
	static final int DMP_QT2H    = 196;
	static final int DMP_QT2L    = 198;
	static final int DMP_QT3H    = 200;
	static final int DMP_QT3L    = 202;
	static final int DMP_QT4H    = 204;
	static final int DMP_QT4L    = 206;
	static final int DMP_CTRL1H    = 208;
	static final int DMP_CTRL1L    = 210;
	static final int DMP_CTRL2H    = 212;
	static final int DMP_CTRL2L    = 214;
	static final int DMP_CTRL3H    = 216;
	static final int DMP_CTRL3L    = 218;
	static final int DMP_CTRL4H    = 220;
	static final int DMP_CTRL4L    = 222;
	static final int DMP_CTRLS1    = 224;
	static final int DMP_CTRLSF1    = 226;
	static final int DMP_CTRLS2    = 228;
	static final int DMP_CTRLSF2    = 230;
	static final int DMP_CTRLS3    = 232;
	static final int DMP_CTRLSFNLL    = 234;
	static final int DMP_CTRLS4    = 236;
	static final int DMP_CTRLSFNL2    = 238;
	static final int DMP_CTRLSFNL    = 240;
	static final int DMP_TMP30    = 242;
	static final int DMP_CTRLSFJT    = 244;
	static final int DMP_TMP31    = 246;
	static final int DMP_TMP11    = 248;
	static final int DMP_CTRLSF2_2    = 250;
	static final int DMP_TMP12    = 252;
	static final int DMP_CTRLSF1_2    = 254;
	static final int DMP_PREVPTAT    = 256;
	static final int DMP_ACCZB    = 258;
	static final int DMP_ACCXB    = 264;
	static final int DMP_ACCYB    = 266;
	static final int DMP_1HB    = 272;
	static final int DMP_1LB    = 274;
	static final int DMP_0H    = 276;
	static final int DMP_0L    = 278;
	static final int DMP_ASR22H    = 280;
	static final int DMP_ASR22L    = 282;
	static final int DMP_ASR6H    = 284;
	static final int DMP_ASR6L    = 286;
	static final int DMP_TMP13    = 288;
	static final int DMP_TMP14    = 290;
	static final int DMP_FINTXH    = 292;
	static final int DMP_FINTXL    = 294;
	static final int DMP_FINTYH    = 296;
	static final int DMP_FINTYL    = 298;
	static final int DMP_FINTZH    = 300;
	static final int DMP_FINTZL    = 302;
	static final int DMP_TMP1BH    = 304;
	static final int DMP_TMP1BL    = 306;
	static final int DMP_TMP2BH    = 308;
	static final int DMP_TMP2BL    = 310;
	static final int DMP_TMP3BH    = 312;
	static final int DMP_TMP3BL    = 314;
	static final int DMP_TMP4BH    = 316;
	static final int DMP_TMP4BL    = 318;
	static final int DMP_STXG    = 320;
	static final int DMP_ZCTXG    = 322;
	static final int DMP_STYG    = 324;
	static final int DMP_ZCTYG    = 326;
	static final int DMP_STZG    = 328;
	static final int DMP_ZCTZG    = 330;
	static final int DMP_CTRLSFJT2    = 332;
	static final int DMP_CTRLSFJTCNT    = 334;
	static final int DMP_PVXG    = 336;
	static final int DMP_TMP15    = 338;
	static final int DMP_PVYG    = 340;
	static final int DMP_TMP16    = 342;
	static final int DMP_PVZG    = 344;
	static final int DMP_TMP17    = 346;
	static final int DMP_MNMFLAGH    = 352;
	static final int DMP_MNMFLAGL    = 354;
	static final int DMP_MNMTMH    = 356;
	static final int DMP_MNMTML    = 358;
	static final int DMP_MNMTMTHRH    = 360;
	static final int DMP_MNMTMTHRL    = 362;
	static final int DMP_MNMTHRH    = 364;
	static final int DMP_MNMTHRL    = 366;
	static final int DMP_ACCQD4H    = 368;
	static final int DMP_ACCQD4L    = 370;
	static final int DMP_ACCQD5H    = 372;
	static final int DMP_ACCQD5L    = 374;
	static final int DMP_ACCQD6H    = 376;
	static final int DMP_ACCQD6L    = 378;
	static final int DMP_ACCQD7H    = 380;
	static final int DMP_ACCQD7L    = 382;
	static final int DMP_ACCQD0H    = 384;
	static final int DMP_ACCQD0L    = 386;
	static final int DMP_ACCQD1H    = 388;
	static final int DMP_ACCQD1L    = 390;
	static final int DMP_ACCQD2H    = 392;
	static final int DMP_ACCQD2L    = 394;
	static final int DMP_ACCQD3H    = 396;
	static final int DMP_ACCQD3L    = 398;
	static final int DMP_XN2H    = 400;
	static final int DMP_XN2L    = 402;
	static final int DMP_XN1H    = 404;
	static final int DMP_XN1L    = 406;
	static final int DMP_YN2H    = 408;
	static final int DMP_YN2L    = 410;
	static final int DMP_YN1H    = 412;
	static final int DMP_YN1L    = 414;
	static final int DMP_YH    = 416;
	static final int DMP_YL    = 418;
	static final int DMP_B0H    = 420;
	static final int DMP_B0L    = 422;
	static final int DMP_A1H    = 424;
	static final int DMP_A1L    = 426;
	static final int DMP_A2H    = 428;
	static final int DMP_A2L    = 430;
	static final int DMP_SEM1    = 432;
	static final int DMP_FIFOCNT    = 434;
	static final int DMP_SH_TH_X    = 436;
	static final int DMP_PACKET    = 438;
	static final int DMP_SH_TH_Y    = 440;
	static final int DMP_FOOTER    = 442;
	static final int DMP_SH_TH_Z    = 444;
	static final int DMP_TEMP29    = 448;
	static final int DMP_TEMP30    = 450;
	static final int DMP_XACCB_PRE    = 452;
	static final int DMP_XACCB_PREL    = 454;
	static final int DMP_YACCB_PRE    = 456;
	static final int DMP_YACCB_PREL    = 458;
	static final int DMP_ZACCB_PRE    = 460;
	static final int DMP_ZACCB_PREL    = 462;
	static final int DMP_TMP22    = 464;
	static final int DMP_TAP_TIMER    = 466;
	static final int DMP_TAP_THX    = 468;
	static final int DMP_TAP_THY    = 472;
	static final int DMP_TAP_THZ    = 476;
	static final int DMP_TAPW_MIN    = 478;
	static final int DMP_TMP25    = 480;
	static final int DMP_TMP26    = 482;
	static final int DMP_TMP27    = 484;
	static final int DMP_TMP28    = 486;
	static final int DMP_ORIENT    = 488;
	static final int DMP_THRSH    = 490;
	static final int DMP_ENDIANH    = 492;
	static final int DMP_ENDIANL    = 494;
	static final int DMP_BLPFNMTCH    = 496;
	static final int DMP_BLPFNMTCL    = 498;
	static final int DMP_BLPFNMXH    = 500;
	static final int DMP_BLPFNMXL    = 502;
	static final int DMP_BLPFNMYH    = 504;
	static final int DMP_BLPFNMYL    = 506;
	static final int DMP_BLPFNMZH    = 508;
	static final int DMP_BLPFNMZL    = 510;
}
