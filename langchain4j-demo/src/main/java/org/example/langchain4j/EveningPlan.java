package org.example.langchain4j;

public class EveningPlan {
    private String movie;
    private String meal;
    public EveningPlan(String movie, String meal) {
        this.movie = movie;
        this.meal = meal;
    }

    public String getMovie() {
        return movie;
    }

    public void setMovie(String movie) {
        this.movie = movie;
    }

    public String getMeal() {
        return meal;
    }

    public void setMeal(String meal) {
        this.meal = meal;
    }

    @Override
    public String toString() {
        return "EveningPlan{" +
                "movie='" + movie + '\'' +
                ", meal='" + meal + '\'' +
                '}';
    }
}
