package x.mvmn.sonivm.impl;

import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import x.mvmn.sonivm.UpdatesService;
import x.mvmn.sonivm.util.ver.SemanticVersion;
import x.mvmn.sonivm.util.ver.SemanticVersionUtil;

@Service
public class UpdatesServiceImpl implements UpdatesService {

	private static final Logger LOGGER = Logger.getLogger(UpdatesServiceImpl.class.getCanonicalName());

	@Value("${releases.url:https://api.github.com/repos/mvmn/sonivm/releases}")
	protected String releasesUrl = "https://api.github.com/repos/mvmn/sonivm/releases";

	@Autowired
	protected SemanticVersion sonivmVersion;

	public String getUpdateLink() {
		String result = null;

		LOGGER.info("Checking for updates");

		Map<SemanticVersion, String> releases = getReleases();
		if (releases != null) {
			result = releases.entrySet()
					.stream()
					.filter(e -> sonivmVersion.compareTo(e.getKey()) < 0)
					.max(Comparator.comparing(Map.Entry::getKey))
					.map(Map.Entry::getValue)
					.orElse(null);
		}

		return result;
	}

	private Map<SemanticVersion, String> getReleases() {
		try {
			Map<SemanticVersion, String> result = new TreeMap<>();
			JsonNode rootNode = new ObjectMapper().readTree(new URL(releasesUrl).openConnection().getInputStream());
			if (!rootNode.isArray()) {
				throw new Exception("Response is not a JSON Array");
			}
			for (final JsonNode elementNode : rootNode) {
				if (elementNode.has("url") && elementNode.has("tag_name")) {
					result.put(SemanticVersionUtil.parse(elementNode.get("tag_name").asText()), elementNode.get("html_url").asText());
				}
			}
			return result;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get update info", e);
		}
		return null;
	}
}
