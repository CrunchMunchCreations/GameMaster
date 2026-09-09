package xyz.crunchmunch.mods.gamemaster.team

class TeamBasedPropertyAccess<T>(val property: TeamPropertyType<T>) {
    operator fun get(team: Team): T {
        return team.getPropertyOrThrow(this.property)
    }

    operator fun set(team: Team, value: T) {
        team.setProperty(this.property, value)
    }

    fun getOrDefault(team: Team, default: T): T {
        return team.getProperty(this.property, default)!!
    }

    fun remove(team: Team) {
        team.removeProperty(this.property)
    }

    fun reset(teamManager: TeamManager) {
        for (team in teamManager) {
            remove(team)
        }
    }
}
