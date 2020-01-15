/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data.clinvar.struct;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClinvarVariantSummary {

    public enum GuidelineType {

        NONE("None"),

        ACMG2013("ACMG2013"),

        ACMG2016("ACMG2016");

        private final String strValue;

        GuidelineType(String strValue) {
            this.strValue = strValue;
        }

        public String getStrValue() {
            return strValue;
        }
    }

    /**
     * https://www.ncbi.nlm.nih.gov/clinvar/docs/review_status/#revstat_def
     */
    public enum ReviewStatus {

        CRITERIA_PROVIDED_CONFLICTIONG_INTERPRETATIONS(
                "criteria provided, conflicting interpretations", true, true, 1
        ),

        CRITERIA_PROVIDED_MULTIPLE_SUBMITTERS_NO_CONFLICTS(
                "criteria provided, multiple submitters, no conflicts", true, false, 2
        ),

        CRITERIA_PROVIDED_SINGLE_SUBMITTER(
                "criteria provided, single submitter", true, null, 1
        ),

        NO_ASSERTION_CRITERIA_PROVIDED(
                "no assertion criteria provided", false, null, 0
        ),

        NO_ASSERTION_PROVIDED(
                "no assertion provided", false, null, 0
        ),

        NO_INTERPRETATION_FOR_THE_SINGLE_VARIANT(
                "no interpretation for the single variant", null, null, 0
        ),

        PRACTICE_GUIDELINE(
                "practice guideline", true, false, 4
        ),

        REVIEWED_BY_EXPERT_PANEL(
                "reviewed by expert panel", true, false, 3
        );

        public final String text;
        public final Boolean сriteriaProvided;
        public final Boolean conflicts;
        public final int stars;

        ReviewStatus(String text, Boolean сriteriaProvided, Boolean conflicts, int stars) {
            this.text = text;
            this.сriteriaProvided = сriteriaProvided;
            this.conflicts = conflicts;
            this.stars = stars;
        }

        public Boolean getCriteriaProvided() {
            return сriteriaProvided;
        }

        public Boolean getConflicts() {
            return conflicts;
        }

        public int getStars() {
            return stars;
        }

        public static ReviewStatus of(String text) {
            for (ReviewStatus item : ReviewStatus.values()) {
                if (item.text.equals(text)) {
                    return item;
                }
            }
            throw new RuntimeException("ReviewStatus not found");
        }
    }


    public final ReviewStatus reviewStatus;
    public final Integer numberSubmitters;
    public List<GuidelineType> guidelineTypes;

    public ClinvarVariantSummary(String reviewStatus, Integer numberSubmitters, String guidelines) {
        this.reviewStatus = ReviewStatus.of(reviewStatus);

        this.numberSubmitters = numberSubmitters;

        if (Strings.isNullOrEmpty(guidelines)) {
            guidelineTypes = ImmutableList.of(GuidelineType.NONE);
        } else {
            guidelineTypes = Arrays.stream(guidelines.split(","))
                    .map(s -> GuidelineType.valueOf(s.trim()))
                    .collect(Collectors.toList());
        }
    }
}
