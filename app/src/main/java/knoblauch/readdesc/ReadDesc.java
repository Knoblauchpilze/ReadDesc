package knoblauch.readdesc;

public class ReadDesc {

    private String m_name;
    private String m_source;
    private String m_date;

    private float m_completionPercentage;

    public ReadDesc(String name, String source, String date) {
        m_name = name;
        m_source = source;
        m_date = date;

        m_completionPercentage = 100.0f * (float)Math.random();
    }

    public String getName() {
        return m_name;
    }

    public String getSource() {
        return m_source;
    }

    public String getDate() {
        return m_date;
    }

    public float getCompletionPercentage() {
        return m_completionPercentage;
    }
}
