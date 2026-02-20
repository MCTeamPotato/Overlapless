# 1.0.0
- Initial release
# 1.0.1
- No longer requires Duplicationless as dep.
# 1.0.2
- Introduce config option "UnskippableStructures": These will always generate even if there are existing structures that occupy the chunk sections.
# 1.0.3
- Resolve a concurrent issue during existing structures' iteration
# 1.0.4
- Resolve https://github.com/MCTeamPotato/Overlapless/issues/1
# 1.0.5
- Minor compatibility improvement for custom WorldGenLevel in other mods
# 1.0.6
- Code refactor. No actual changes for players.
- Port to 1.16.5 and 1.18.2, as well as many subversions 1.21.x
# 1.1.0
- Introduce SkippableFeatures config option. You can write down the features' registry names here so they will not get overlapped with existing structures.
- Introduce PrintStructureSkipEventInLog and PrintFeatureSkipEventInLog config option
# 1.1.1
- Resolve https://github.com/MCTeamPotato/Overlapless/issues/2