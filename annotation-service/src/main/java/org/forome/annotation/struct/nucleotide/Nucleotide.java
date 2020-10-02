/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.struct.nucleotide;

public enum Nucleotide {

	NONE('N', false),

	A('A', true),//Аденин
	G('G', true),//Гуанин
	C('C', true),//Цитозин
	T('T', true),//Тимин

	a('a', false),//Аденин
	g('g', false),//Гуанин
	c('c', false),//Цитозин
	t('t', false);//Тимин

	public final char character;
	public final boolean conservative;

	Nucleotide(char character, boolean conservative) {
		this.character = character;
		this.conservative = conservative;
	}

	public static Nucleotide of(char character){
		for (Nucleotide item: Nucleotide.values()) {
			if (item.character==character) {
				return item;
			}
		}
		throw new RuntimeException("Not support char: " + character);
	}
}
