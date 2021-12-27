package x.mvmn.sonivm.util;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class IntRange implements Serializable {
	private static final long serialVersionUID = -4261152720279080615L;
	private final int from;
	private final int to;
}
