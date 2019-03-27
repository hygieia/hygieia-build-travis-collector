package com.capitalone.dashboard.model;

public class TravisJob extends JobCollectorItem {
    public static final String REPO_BRANCH = "branch";
    public static final String PERSONAL_ACCESS_TOKEN = "personalAccessToken";

    public String getPersonalAccessToken() { return (String) getOptions().get(PERSONAL_ACCESS_TOKEN); }

    public void setPersonalAccessToken(String personalAccessToken) { getOptions().put(PERSONAL_ACCESS_TOKEN, personalAccessToken); }

    public String getRepoBranch() {
        return (String) getOptions().get(REPO_BRANCH);
    }
    public void setRepoBranch(String repoBranch) { getOptions().put(REPO_BRANCH, repoBranch); }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TravisJob travisJob = (TravisJob) o;

        return getJobUrl().equals(travisJob.getJobUrl()) && getJobName().equals(travisJob.getJobName());
    }

    @Override
    public int hashCode() {
        int result = getJobUrl().hashCode();
        result = 31 * result + getJobName().hashCode();
        return result;
    }
}
