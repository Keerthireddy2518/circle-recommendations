package com.leanin.circle_recommendations.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "circle.recommendation")
public class RecommendationScoringProperties {

    private double topicMatchWeight = 0.45;
    private double activityWeight = 0.20;
    private double engagementWeight = 0.15;
    private double recencyWeight = 0.10;
    private double popularityWeight = 0.10;
    private double coldStartWeight = 0.35;
    private int maxMembersPerCircle = 25;
    private int inactiveDaysThreshold = 90;
    private int maxResults = 10;
    private double nearDuplicateSimilarityThreshold = 0.80;
    private int recentActivityWindowDays = 30;

    public double getTopicMatchWeight() {
        return topicMatchWeight;
    }

    public void setTopicMatchWeight(double topicMatchWeight) {
        this.topicMatchWeight = topicMatchWeight;
    }

    public double getActivityWeight() {
        return activityWeight;
    }

    public void setActivityWeight(double activityWeight) {
        this.activityWeight = activityWeight;
    }

    public double getEngagementWeight() {
        return engagementWeight;
    }

    public void setEngagementWeight(double engagementWeight) {
        this.engagementWeight = engagementWeight;
    }

    public double getRecencyWeight() {
        return recencyWeight;
    }

    public void setRecencyWeight(double recencyWeight) {
        this.recencyWeight = recencyWeight;
    }

    public double getPopularityWeight() {
        return popularityWeight;
    }

    public void setPopularityWeight(double popularityWeight) {
        this.popularityWeight = popularityWeight;
    }

    public double getColdStartWeight() {
        return coldStartWeight;
    }

    public void setColdStartWeight(double coldStartWeight) {
        this.coldStartWeight = coldStartWeight;
    }

    public int getMaxMembersPerCircle() {
        return maxMembersPerCircle;
    }

    public void setMaxMembersPerCircle(int maxMembersPerCircle) {
        this.maxMembersPerCircle = maxMembersPerCircle;
    }

    public int getInactiveDaysThreshold() {
        return inactiveDaysThreshold;
    }

    public void setInactiveDaysThreshold(int inactiveDaysThreshold) {
        this.inactiveDaysThreshold = inactiveDaysThreshold;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public double getNearDuplicateSimilarityThreshold() {
        return nearDuplicateSimilarityThreshold;
    }

    public void setNearDuplicateSimilarityThreshold(double nearDuplicateSimilarityThreshold) {
        this.nearDuplicateSimilarityThreshold = nearDuplicateSimilarityThreshold;
    }

    public int getRecentActivityWindowDays() {
        return recentActivityWindowDays;
    }

    public void setRecentActivityWindowDays(int recentActivityWindowDays) {
        this.recentActivityWindowDays = recentActivityWindowDays;
    }
}
